package callprotector.spring.global.handler;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import callprotector.spring.global.client.FastClient;
import callprotector.spring.global.common.enums.CallTrack;
import callprotector.spring.domain.calllog.service.CallLogService;
import callprotector.spring.domain.callsession.service.CallSessionService;
import callprotector.spring.domain.callsttlog.service.CallSttLogService;
import callprotector.spring.domain.user.service.UserService;
import callprotector.spring.domain.callsession.dto.request.CallSessionRequestDTO;

import callprotector.spring.global.multimodal.ShoutingDetector;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public class TwilioMediaStreamProcessor {
	private final ObjectMapper mapper;
	private final FastClient fastClient;
	private final CallSessionService callSessionService;
	private final CallLogService callLogService;
	private final CallSttLogService callSttLogService;
	private final UserService userService;
	private final ClientNotifier sttWebSocketHandler;
	private final TwilioSessionManager twilioSessionManager;
	private final ShoutingDetector shoutingDetector;

	private final Map<CallTrack, SttContext> sttContexts = new ConcurrentHashMap<>();

	private Long currentUserId;
	private Long currentCallSessionId;
	private String primaryCallSid;

	private static final long STREAM_RESTART_INTERVAL_MS = 10_000;
	private static final long TEMP_USERID = 1;

	public void handleTwilioMessage(WebSocketSession session, TextMessage message) throws Exception {
		JsonNode json = mapper.readTree(message.getPayload());

		if (json.has("event") && "start".equals(json.get("event").asText())) {
			handleStartEvent(session, json);
		} else if (json.has("event") && "media".equals(json.get("event").asText())) {
			handleMediaEvent(session, json);
		}
	}

	public void handleError(Throwable exception) {
		log.error("TwilioMediaStreamProcessor에서 오류 발생. CallSessionId: {}", currentCallSessionId, exception);
	}

	public void closeSession() {
		log.info("Closing STTContexts for CallSessionId: {}", currentCallSessionId);
		// sttContexts 맵에 저장된 모든 STTContext 인스턴스에 대해 closeStream() 호출
		sttContexts.values().forEach(SttContext::closeStream);

		// ShoutingDetector 리소스 정리
		if (this.shoutingDetector != null) {
			this.shoutingDetector.close();
		}

		// CallSession의 endedAt 필드 업데이트
		if (currentCallSessionId == null) {
			log.warn("CallSession ID가 null이므로, endedAt을 업데이트할 수 없습니다.");
			return;
		}

		callSessionService.updateEndedAt(currentCallSessionId);

		// 세션 관련 정보 초기화
		currentUserId = null;
		currentCallSessionId = null;
		sttContexts.clear();
		log.info("✅ CallSessionId {}의 TwilioMediaStreamProcessor 정리 완료.", currentCallSessionId);
	}

	public void updateUserId(Long newUserId) {
		if (this.currentUserId == null || !this.currentUserId.equals(newUserId)) {
			log.info("CallSessionId {}의 userId가 {}에서 {}로 업데이트됩니다.",
				this.currentCallSessionId, this.currentUserId, newUserId);
			this.currentUserId = newUserId;

			// SttContext의 userId 업데이트
			sttContexts.values().forEach(ctx -> ctx.updateUserId(newUserId));

			log.info("CallSessionId {}의 userId를 {}로 업데이트 왑료", this.currentCallSessionId, this.currentUserId);
		}
	}

	public void handleCallAccepted() {
		try {
			// ShoutingDetector 초기화
			shoutingDetector.initializeTarsosDSP(8000);
			log.info("✅ 전화 수락 이벤트 수신. ShoutingDetector 초기화 완료.");
		} catch (IOException e) {
			log.error("ShoutingDetector 초기화 중 오류 발생", e);
		}
	}

	private void handleStartEvent(WebSocketSession session, JsonNode json) {
		log.info("☆ Twilio Media Stream 'start' 이벤트 전체 JSON: {}", json.toPrettyString());
		JsonNode customParams = json.path("start").path("customParameters");
		String primaryCallSid = customParams.path("primaryCallSid").asText(); // 인바운드 통화 CallSid (고객의 최초 CallSid)
		String callerNumber = customParams.path("callerNumber").asText();

		this.primaryCallSid = primaryCallSid;

		// 프로세스 인스턴스 등록
		twilioSessionManager.registerProcessor(this.primaryCallSid, this);

		// callSession 객체 생성
		currentCallSessionId = callSessionService.createTempSession(
			TEMP_USERID,
			new CallSessionRequestDTO.CallSessionMakeDTO(primaryCallSid, callerNumber)
		);

		try {
			// INBOUND STTContext 생성 및 초기화
			SttContext inboundCtx = new SttContext(
				currentCallSessionId,
				currentUserId,
				CallTrack.INBOUND,
				fastClient,
				callSessionService,
				callLogService,
				callSttLogService,
				sttWebSocketHandler
			);
			inboundCtx.initializeStream(session.getId());
			sttContexts.put(CallTrack.INBOUND, inboundCtx);

			// ShoutingDetector에 INBOUND SttContext 연결
			shoutingDetector.setSttContext(inboundCtx);

			// OUTBOUND STTContext 생성 및 초기화
			SttContext outboundCtx = new SttContext(
				currentCallSessionId,
				currentUserId,
				CallTrack.OUTBOUND,
				fastClient,
				callSessionService,
				callLogService,
				callSttLogService,
				sttWebSocketHandler
			);
			outboundCtx.initializeStream(session.getId());
			sttContexts.put(CallTrack.OUTBOUND, outboundCtx);

		} catch (IOException e) {
			log.error("세션 {}에 대한 STT 컨텍스트 초기화 실패", session.getId(), e);
			throw new RuntimeException("STT Context 초기화 실패", e);
		}
	}

	private void handleMediaEvent(WebSocketSession session, JsonNode json) throws IOException {
		String trackRaw = json.get("media").get("track").asText();
		CallTrack track = CallTrack.valueOf(trackRaw.toUpperCase());

		String base64 = json.get("media").get("payload").asText();
		byte[] audio = Base64.getDecoder().decode(base64);

		SttContext ctx = sttContexts.get(track);
		if (ctx == null) {
			log.warn("❗ 세션 {}의 트랙 {}에 대한 STTContext를 찾을 수 없습니다.", session.getId(), track);
			return;
		}

		// SttContext에 오디오 데이터 처리
		ctx.processAudio(audio);

		// ShoutingDetector에 오디오 데이터 전달 (INBOUND 트랙만 분석)
		if (track == CallTrack.INBOUND) {
			try {
				shoutingDetector.transferAudio(audio);
			} catch (IOException e) {
				log.error("ShoutingDetector에서 오디오 처리 중 오류 발생. CallSessionId: {}", currentCallSessionId, e);
			}
		}

		long now = System.currentTimeMillis();

		// 10초마다 stream 재시작 + 재시작 전 마지막 중간 텍스트 -> 강제 최종 텍스트로 처리
		if (track == CallTrack.INBOUND && now - ctx.getLastStreamStartTime() >= STREAM_RESTART_INTERVAL_MS) {
			ctx.restartStream(session.getId());
		}
	}
}
