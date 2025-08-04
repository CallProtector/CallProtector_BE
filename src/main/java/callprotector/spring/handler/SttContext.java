package callprotector.spring.handler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.StreamController;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;

import callprotector.spring.global.client.FastClient;
import callprotector.spring.domain.CallSttLog;
import callprotector.spring.domain.enums.CallTrack;
import callprotector.spring.service.CallLogService.CallLogService;
import callprotector.spring.service.CallSessionService.CallSessionService;
import callprotector.spring.service.CallSttLogService.CallSttLogService;
import callprotector.spring.web.dto.response.CallSttLogResponseDTO;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public class SttContext {
	private final Long callSessionId;
	private final Long userId;
	private final CallTrack track;

	private final FastClient fastClient;
	private final CallSessionService callSessionService;
	private final CallLogService callLogService;
	private final CallSttLogService callSttLogService;
	private final ClientNotifier sttWebSocketHandler;

	private SpeechClient client;
	private ClientStream<StreamingRecognizeRequest> stream;
	private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	private long lastSendTime = System.currentTimeMillis();
	private long lastStreamStartTime = System.currentTimeMillis();
	private StringBuilder transcriptBuilder = new StringBuilder();
	private String partialFinalTranscript;
	private String lastSavedFinalTranscript;

	private static final Boolean NOT_ABUSIVE = false;
	private static final String ABUSIVE_TYPE_NORMAL = "정상";
	private static final String DATA_TYPE_STT = "stt";

	// Google STT 스트림 초기화
	public void initializeStream(String sessionId) throws IOException {
		client = SpeechClient.create();

		RecognitionConfig config = RecognitionConfig.newBuilder()
			.setEncoding(RecognitionConfig.AudioEncoding.MULAW)
			.setSampleRateHertz(8000)
			.setLanguageCode("ko-KR")
			.build();

		StreamingRecognitionConfig streamingConfig = StreamingRecognitionConfig.newBuilder()
			.setConfig(config)
			.setInterimResults(true)
			.build();

		ResponseObserver<StreamingRecognizeResponse> observer = createResponseObserver(sessionId);

		stream = client.streamingRecognizeCallable().splitCall(observer);
		stream.send(StreamingRecognizeRequest.newBuilder()
			.setStreamingConfig(streamingConfig)
			.build());

		lastStreamStartTime = System.currentTimeMillis();
	}

	// Twilio로부터 받은 오디오 데이터를 버퍼에 쓰고 Google STT로 전송
	public void processAudio(byte[] audio) throws IOException {
		buffer.write(audio);
		long now = System.currentTimeMillis();

		// 200ms마다 오디오 청크 전송
		if (now - lastSendTime >= 200) {
			byte[] chunk = buffer.toByteArray();
			stream.send(StreamingRecognizeRequest.newBuilder()
				.setAudioContent(ByteString.copyFrom(chunk))
				.build());
			buffer.reset();
			lastSendTime = now;
		}
	}

	// Google STT 스트림 재시작
	public void restartStream(String sessionId) throws IOException {
		try {
			buffer.reset();

			// 마지막 중간 텍스트를 강제로 최종 텍스트로 처리
			if (partialFinalTranscript != null && !partialFinalTranscript.trim().isEmpty()) {
				String forcedFinal = partialFinalTranscript.trim();
				log.info("[강제 최종] 중간 결과를 최종으로 처리: {}", forcedFinal);

				try {
					// 중복 저장 방지
					boolean isDuplicate = forcedFinal.equals(lastSavedFinalTranscript)
						|| transcriptBuilder.toString().contains(forcedFinal);

					if (track == CallTrack.INBOUND && isDuplicate) {
						log.debug("[중복 제거] 강제 저장 생략: {}", forcedFinal);
					} else {
						// 욕설 분석 요청
						var result = fastClient.sendTextToFastAPI(forcedFinal);

						// stt 로그 저장
						CallSttLog savedLog = callSttLogService.saveTranscriptLog(
							callSessionId,
							track,
							forcedFinal,
							true,
							result.isAbuse(),
							result.getType()
						);

						if (track == CallTrack.INBOUND) {
							transcriptBuilder.append(forcedFinal).append(" ");
							lastSavedFinalTranscript = forcedFinal;
						}

						// 클라이언트에 STT 로그 전송
						CallSttLogResponseDTO forcedResponse = new CallSttLogResponseDTO(DATA_TYPE_STT, savedLog);
						if (userId != null) {
							sttWebSocketHandler.sendSttToClient(userId, forcedResponse);
						}

						// INBOUND 트랙만 욕설 처리
						// 욕설 감지 시 CallSession의 totalAbuseCnt 업데이트
						if (result.isAbuse() && track == CallTrack.INBOUND) {
							log.info("STT 결과 욕설 감지 - (isAbuse={}) / CallSession total_abuse_cnt 업데이트 시도 - CallSessionId={}", result.isAbuse(), callSessionId);
							callSessionService.incrementTotalAbuseCnt(callSessionId);
							callLogService.updateAbuse(callSessionId, track);
						}
					}
				} catch (Exception e) {
					log.warn("❗ 강제 final 처리 실패", e);
				}

				// 처리 후 초기화
				partialFinalTranscript = null;
			}

			// 기존 스트림 종료
			if (stream != null) {
				stream.closeSend();
			}

			// 새 스트림 초기화
			initializeStream(sessionId);
			log.info("🔄 [{}] Google STT Stream 재시작됨", track);

		} catch (Exception e) {
			log.error("STT 재시작 중 오류", e);
		}
	}

	// STT 스트림을 종료하고, 종료 시점에 남아있는 중간 텍스트를 최종 텍스트로 처리 및 저장
	public void closeStream() {
		if (stream != null) {
			try {
				stream.closeSend();
				Thread.sleep(300);

				if (client != null) {
					client.shutdown();
					if (!client.awaitTermination(1, TimeUnit.SECONDS)) {
						client.shutdownNow(); // 강제 종료
					}
				}
				log.info("✅ [{}] STT 스트림 종료 완료 - CallSessionId: {}", track, callSessionId);

				// 종료 시점에 남은 텍스트 처리
				processRemainingTranscript();

			} catch (Exception e) {
				log.error("❌ [{}] STT 종료 중 오류", track, e);
			}
		}
	}

	// 스트림 종료 시 또는 재시작 시점에 남아있는 중간 텍스트를 최종 텍스트로 처리하고 저장
	private void processRemainingTranscript() {
		String finalTranscript = transcriptBuilder.toString().trim();

		// 마지막 중간 텍스트가 존재하고, 아직 처리되지 않았다면 최종 텍스트에 추가
		if (partialFinalTranscript != null && !partialFinalTranscript.trim().isEmpty()) {
			String fallback = partialFinalTranscript.trim();

			// 중복 방지 - 이미 최종으로 처리된 것과 동일한 경우
			if (track == CallTrack.INBOUND && fallback.equals(lastSavedFinalTranscript)) {
				log.debug("[종료시 중복 제거] 동일한 partialFinalTranscript 생략: {}", fallback);
			} else {
				log.info("[종료시 처리] 누락 가능 중간 텍스트 처리: {}", fallback);
				finalTranscript += (finalTranscript.isEmpty() ? "" : " ") + fallback;

				// INBOUND인 경우 기록해 중복 방지
				if (track == CallTrack.INBOUND) {
					lastSavedFinalTranscript = fallback;
				}
			}
		}

		// 예외 케이스 보완 - transcriptBuilder에 내용이 있지만 finalTranscript가 비어있는 경우
		if (finalTranscript.isEmpty() && transcriptBuilder.length() > 0) {
			finalTranscript = transcriptBuilder.toString().trim();
		}

		if (!finalTranscript.isEmpty()) {
			log.info("📝 [{}] 전체 텍스트: {}", track == CallTrack.INBOUND ? "고객" : "상담원", finalTranscript);
		}

		if (track == CallTrack.INBOUND) {
			try {
				// 세션 내 기존 욕설 여부 확인
				boolean hasAbuseInSttLog = callSttLogService.hasAbuseInSession(callSessionId);

				// FastAPI를 통한 욕설 분석
				var inboundResult = fastClient.sendTextToFastAPI(finalTranscript);
				log.info("⚠️ [{}] INBOUND 욕설 감지 결과 → isAbuse: {}, type: {}",
					CallTrack.INBOUND, inboundResult.isAbuse(), inboundResult.getType());

				boolean finalAbuse = hasAbuseInSttLog || inboundResult.isAbuse();

				String finalAbuseType = hasAbuseInSttLog
					? callSttLogService.getAbuseTypesBySessionId(callSessionId)
					: inboundResult.getType();

				// 최종 텍스트를 CallLog에 저장
				callLogService.saveFinalTranscript(
					callSessionId,
					track,
					finalTranscript,
					finalAbuse,
					finalAbuseType
				);
			} catch (Exception e) {
				log.error(" [{}] 욕설 분석 실패", track, e);
			}
		} else { // OUTBOUND (상담원) 발화 욕설 분석 생략
			log.info("ℹ️ [{}] 상담원 발화는 욕설 분석을 건너뜁니다.", track);
			callLogService.saveFinalTranscript(
				callSessionId,
				track,
				finalTranscript,
				NOT_ABUSIVE,
				ABUSIVE_TYPE_NORMAL
			);
		}

		// 클라이언트에 최종 텍스트 전송
		if (userId != null) {
			sttWebSocketHandler.sendSttToClient(userId, Map.of(
				"type", "finalTranscript",
				"track", track.name(),
				"text", finalTranscript
			));
		}
	}

	// Google STT 응답을 처리하는 ResponseObserver 생성
	private ResponseObserver<StreamingRecognizeResponse> createResponseObserver(String sessionId){
		return new ResponseObserver<>() {
			public void onStart(StreamController controller) {
				log.info("🎤 STT 시작됨: {} [{}]", sessionId, track);
			}

			public void onResponse(StreamingRecognizeResponse response) {
				for (StreamingRecognitionResult result : response.getResultsList()) {
					if (result.getAlternativesCount() > 0) {
						String transcript = result.getAlternatives(0).getTranscript();
						boolean isFinal = result.getIsFinal();

						log.info("💬 [{}][{}] {}", isFinal ? "최종" : "중간",
							track == CallTrack.INBOUND ? "고객" : "상담원",
							transcript);

						try {
							if (isFinal) { // 최종 결과
								String trimmedTranscript = transcript.trim();

								// 중복 방지 - 이전 저장값과 동일한 경우 저장 생략
								if (track == CallTrack.INBOUND && trimmedTranscript.equals(lastSavedFinalTranscript)) {
									log.debug("[중복 제거] 동일한 최종 텍스트는 무시됨: {}", trimmedTranscript);
									continue;
								}

								boolean isAbuse = false;
								String abuseType = ABUSIVE_TYPE_NORMAL;

								if(track == CallTrack.INBOUND) {
									var analysis = fastClient.sendTextToFastAPI(trimmedTranscript);
									isAbuse = analysis.isAbuse();
									abuseType = analysis.getType();
									if (isAbuse) {
										log.info("[{}] INBOUND 욕설 감지 결과 → isAbuse: {}, type: {}", CallTrack.INBOUND, isAbuse, abuseType);
									}
								} else { // // OUTBOUND 트랙은 욕설 분석 건너뜀
									log.info("[{}] 상담원 발화는 욕설 분석을 건너뜀", track);

								}

								// sttLog 최종 저장
								CallSttLog savedLog = callSttLogService.saveTranscriptLog(
									callSessionId,
									track,
									trimmedTranscript,
									true,
									isAbuse,
									abuseType
								);
								log.info("MongoDB에 CallSttLog 저장 완료: callSessionId={}, track={}, script={}", callSessionId, track, trimmedTranscript);


								if (track == CallTrack.INBOUND) {
									// transcriptBuilder 중복 누적 방지
									if (!trimmedTranscript.equals(lastSavedFinalTranscript)) {
										transcriptBuilder.append(trimmedTranscript).append(" ");
										lastSavedFinalTranscript = trimmedTranscript;
									}

									// 욕설 감지 시 total abuse cnt 업데이트
									if (isAbuse) {
										log.info("STT 결과 욕설 감지 - (isAbuse={}) / CallSession total_abuse_cnt 업데이트 시도 - CallSessionId={}", isAbuse, callSessionId);
										callSessionService.incrementTotalAbuseCnt(callSessionId);
										log.info("🍀 고객 발화 필터링됨");
										callLogService.updateAbuse(callSessionId, track);
									}
								}

								// 클라이언트에 최종 STT 결과 전송
								CallSttLogResponseDTO finalResponse = new CallSttLogResponseDTO(DATA_TYPE_STT, savedLog);

								if (userId != null) {
									sttWebSocketHandler.sendSttToClient(userId, finalResponse);
								}

								partialFinalTranscript = null; // 최종 처리 후 중간 텍스트 초기화

							} else { // 중간 결과는 DB 저장 없이 클라이언트 뷰에만 보여줌
								CallSttLog interimLog = CallSttLog.builder()
									.callSessionId(callSessionId)
									.track(track)
									.script(transcript)
									.isFinal(false)
									.isAbuse(false)
									.abuseType("정상")
									.abuseCnt(0)
									.build();

								// 클라이언트에 중간 STT 결과 전송
								CallSttLogResponseDTO interimResponse = new CallSttLogResponseDTO(DATA_TYPE_STT, interimLog);

								if (userId != null) {
									sttWebSocketHandler.sendSttToClient(userId, interimResponse);
								}

								partialFinalTranscript = transcript;
							}

						} catch (Exception e) {
							log.error("❌ FastAPI 전송 오류", e);
						}
					}
				}
			}

			public void onError(Throwable t) {
				log.error("[{}] STT 오류", track, t);
			}

			public void onComplete() {
				log.info("[{}] STT 완료", track);
			}
		};
	}
}
