package callprotector.spring.config;

import callprotector.spring.client.FastClient;
import callprotector.spring.domain.CallSession;
import callprotector.spring.domain.CallSttLog;
import callprotector.spring.domain.enums.CallTrack;
import callprotector.spring.service.CallLogService.CallLogService;
import callprotector.spring.service.CallSessionService.CallSessionService;
import callprotector.spring.service.CallSttLogService.CallSttLogService;
import callprotector.spring.web.dto.request.CallSessionRequestDTO;
import callprotector.spring.web.dto.response.CallSessionResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.StreamController;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;

import callprotector.spring.web.dto.response.CallSttLogResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Component
@Slf4j
public class TwilioMediaStreamsHandler extends AbstractWebSocketHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private final FastClient fastClient;
    private final CallSessionService callSessionService;
    private final CallLogService callLogService;
    private final CallSttLogService callSttLogService;
    private final SttWebSocketHandler sttWebSocketHandler;

    private static final long STREAM_RESTART_INTERVAL_MS = 10_000;
    private static final Boolean NOT_ABUSIVE = false;
    private static final String ABUSIVE_TYPE_NORMAL = "정상";
    private static final String DATA_TYPE_STT = "stt";

    private static class STTContext {
        SpeechClient client;
        ClientStream<StreamingRecognizeRequest> stream;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        long lastSendTime = System.currentTimeMillis();
        long lastStreamStartTime = System.currentTimeMillis();
        StringBuilder transcriptBuilder = new StringBuilder();
        WebSocketSession session;
        Long callSessionId;
        CallSession callSession;
        Long userId;
        String partialFinalTranscript;
        String lastSavedFinalTranscript;
    }

    private final Map<String, STTContext> inboundMap = new ConcurrentHashMap<>();
    private final Map<String, STTContext> outboundMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("✅ WebSocket 연결됨: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode json = mapper.readTree(message.getPayload());

        // Twilio에서 처음 연결 시 start 이벤트에 userId 포함됨
        if (json.has("event") && "start".equals(json.get("event").asText())) {
            JsonNode customParams = json.path("start").path("customParameters");
            String userIdStr = customParams.path("userId").asText();
            String callerNumber = customParams.path("callerNumber").asText();
            String twilioCallSid = json.path("start").path("callSid").asText();

            if (!userIdStr.isEmpty()) {
                log.info("🎯 Twilio start event에서 받은 userId: {}", userIdStr);
                Long userId = Long.parseLong(userIdStr);

                // callSession 객체 생성
                Long callSessionId = callSessionService.createCallSession(
                        "dlthdal07@gmail.com", // TODO: 사용자 이메일 동적 처리
                        new CallSessionRequestDTO.CallSessionMakeDTO(userId, twilioCallSid, callerNumber)
                );

                // callSessionId로 callSession 객체 조회
                CallSession currentCallSession = callSessionService.getCallSession(callSessionId);


                STTContext inbound = getOrCreateContext(session.getId(), CallTrack.INBOUND, session);
                STTContext outbound = getOrCreateContext(session.getId(), CallTrack.OUTBOUND, session);
                inbound.userId = userId;
                outbound.userId = userId;

                inbound.callSessionId = callSessionId;
                outbound.callSessionId = callSessionId;

                inbound.callSession = currentCallSession;
                outbound.callSession = currentCallSession;

                // 세션 정보 전달 - call_session_code, 날짜 (stt 페이지 상단)
                CallSessionResponseDTO.CallSessionInfoDTO sessionInfo =
                        callSessionService.getCallSessionInfo(callSessionId);
                // 세션 정보 로그로 확인
                log.info("🧾 생성된 CallSession 정보: sessionCode = {}, createdAt = {}, totalAbuseCnt = {}",
                        sessionInfo.getCallSessionCode(), sessionInfo.getCreatedAt(), sessionInfo.getTotalAbuseCnt());

                sttWebSocketHandler.sendSessionInfoToClient(userId, sessionInfo);

            } else {
                log.warn("❗ start 이벤트에 userId 없음");
            }
            return;
        }


        // 실제 음성 데이터 처리
        if (!json.has("event") || !"media".equals(json.get("event").asText())) return;

        String trackRaw = json.get("media").get("track").asText();
        CallTrack track = CallTrack.valueOf(trackRaw.toUpperCase());

        String base64 = json.get("media").get("payload").asText();
        byte[] audio = Base64.getDecoder().decode(base64);

        STTContext ctx = getOrCreateContext(session.getId(), track, session);
        ctx.buffer.write(audio);
        ctx.session = session;

        long now = System.currentTimeMillis();

        // 10초마다 stream 재시작 + 재시작 전 마지막 중간 텍스트 -> 강제 최종 텍스트로 처리
        if (track == CallTrack.INBOUND && now - ctx.lastStreamStartTime >= STREAM_RESTART_INTERVAL_MS) {
            ctx.buffer.reset();
            restartStream(ctx, track, session.getId());
        }

        if (now - ctx.lastSendTime >= 200) {
            byte[] chunk = ctx.buffer.toByteArray();
            ctx.stream.send(StreamingRecognizeRequest.newBuilder()
                    .setAudioContent(ByteString.copyFrom(chunk))
                    .build());
            ctx.buffer.reset();
            ctx.lastSendTime = now;
        }
    }

    private STTContext getOrCreateContext(String sessionId, CallTrack track, WebSocketSession session) throws IOException {
        Map<String, STTContext> targetMap = (track == CallTrack.INBOUND) ? inboundMap : outboundMap;

        return targetMap.computeIfAbsent(sessionId, id -> {
            try {
                STTContext ctx = new STTContext();
                ctx.session = session;

                ctx.client = SpeechClient.create();

                RecognitionConfig config = RecognitionConfig.newBuilder()
                        .setEncoding(RecognitionConfig.AudioEncoding.MULAW)
                        .setSampleRateHertz(8000)
                        .setLanguageCode("ko-KR")
                        .build();

                StreamingRecognitionConfig streamingConfig = StreamingRecognitionConfig.newBuilder()
                        .setConfig(config)
                        .setInterimResults(true)
                        .build();

                ResponseObserver<StreamingRecognizeResponse> observer = createResponseObserver(ctx, track, sessionId);

                ctx.stream = ctx.client.streamingRecognizeCallable().splitCall(observer);
                ctx.stream.send(StreamingRecognizeRequest.newBuilder()
                        .setStreamingConfig(streamingConfig)
                        .build());

                return ctx;
            } catch (IOException e) {
                throw new RuntimeException("STTContext 생성 실패", e);
            }
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("✅ 연결 종료: {}", session.getId());

        closeContext(inboundMap.remove(session.getId()), CallTrack.INBOUND);
        closeContext(outboundMap.remove(session.getId()), CallTrack.OUTBOUND);
    }

    private void closeContext(STTContext ctx, CallTrack track) {
        if (ctx != null) {
            try {
                ctx.stream.closeSend();

                Thread.sleep(300);

                ctx.client.shutdown();
                if (!ctx.client.awaitTermination(1, TimeUnit.SECONDS)) {
                    ctx.client.shutdownNow();
                }

                log.info("✅ [{}] STT 스트림 종료 완료", track);

                String finalTranscript = ctx.transcriptBuilder.toString().trim();

                // ✅ 최종 텍스트 누락 방지 + 중복 제거
                if (ctx.partialFinalTranscript != null && !ctx.partialFinalTranscript.trim().isEmpty()) {
                    String fallback = ctx.partialFinalTranscript.trim();

                    if (track == CallTrack.INBOUND && fallback.equals(ctx.lastSavedFinalTranscript)) {
                        log.debug("⏭️ [종료시 중복 제거] 동일한 partialFinalTranscript 생략: {}", fallback);
                    } else {
                        log.info("💡 [종료시 처리] 누락 가능 중간 텍스트 처리: {}", fallback);
                        finalTranscript += " " + fallback;

                        // ✅ INBOUND인 경우 기록해 중복 방지
                        if (track == CallTrack.INBOUND) {
                            ctx.lastSavedFinalTranscript = fallback;
                        }
                    }
                }

                // 💡 강제 저장 보완
                if (finalTranscript.isEmpty() && ctx.transcriptBuilder.length() > 0) {
                    finalTranscript = ctx.transcriptBuilder.toString().trim();
                }

                if (!finalTranscript.isEmpty()) {
                    log.info("📝 [{}] 전체 텍스트: {}", track == CallTrack.INBOUND ? "고객" : "상담원", finalTranscript);

                    if (track == CallTrack.INBOUND) {
                        try {
                            boolean hasAbuseInSttLog = callSttLogService.hasAbuseInSession(ctx.callSessionId);

                            var inboundResult = fastClient.sendTextToFastAPI(finalTranscript);
                            log.info("⚠️ [{}] INBOUND 욕설 감지 결과 → isAbuse: {}, type: {}",
                                    CallTrack.INBOUND, inboundResult.isAbuse(), inboundResult.getType());

                            boolean finalAbuse = hasAbuseInSttLog || inboundResult.isAbuse();

                            String finalAbuseType = hasAbuseInSttLog
                                    ? callSttLogService.getAbuseTypesBySessionId(ctx.callSessionId)
                                    : inboundResult.getType();

                            callLogService.saveFinalTranscript(
                                    ctx.callSessionId,
                                    track,
                                    finalTranscript,
                                    finalAbuse,
                                    finalAbuseType
                            );

                        } catch (Exception e) {
                            log.error("🚨 [{}] 욕설 분석 실패", track, e);
                        }

                    } else {
                        log.info("ℹ️ [{}] 상담원 발화는 욕설 분석을 건너뜁니다.", track);
                        callLogService.saveFinalTranscript(
                                ctx.callSessionId,
                                track,
                                finalTranscript,
                                NOT_ABUSIVE,
                                ABUSIVE_TYPE_NORMAL
                        );

                    }

                    if (ctx.userId != null) {
                        sttWebSocketHandler.sendSttToClient(ctx.userId, Map.of(
                                "type", "finalTranscript",
                                "track", track.name(),
                                "text", finalTranscript
                        ));
                    }
                }

            } catch (Exception e) {
                log.error("❌ [{}] STT 종료 중 오류", track, e);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("❌ WebSocket 오류: {}", exception.getMessage());
    }

    private void restartStream(STTContext ctx, CallTrack track, String sessionId) {
        try {
            ctx.buffer.reset();

            // ✅ 마지막 중간 텍스트 강제 처리
            if (ctx.partialFinalTranscript != null && !ctx.partialFinalTranscript.trim().isEmpty()) {
                String forcedFinal = ctx.partialFinalTranscript.trim();
                log.info("💡 [강제 최종] 중간 결과를 최종으로 처리: {}", forcedFinal);

                try {
                    boolean isDuplicate = forcedFinal.equals(ctx.lastSavedFinalTranscript)
                            || ctx.transcriptBuilder.toString().contains(forcedFinal);

                    if (track == CallTrack.INBOUND && isDuplicate) {
                        log.debug("⏭️ [중복 제거] 강제 저장 생략: {}", forcedFinal);
                    } else {
                        var result = fastClient.sendTextToFastAPI(forcedFinal);
                        CallSttLog savedLog = callSttLogService.saveTranscriptLog(
                                ctx.callSessionId,
                                track,
                                forcedFinal,
                                true,
                                result.isAbuse(),
                                result.getType()
                        );

                        if (track == CallTrack.INBOUND) {
                            ctx.transcriptBuilder.append(forcedFinal).append(" ");
                            ctx.lastSavedFinalTranscript = forcedFinal;
                        }

                        CallSttLogResponseDTO forcedResponse = new CallSttLogResponseDTO(DATA_TYPE_STT, savedLog);
                        if (ctx.userId != null) {
                            sttWebSocketHandler.sendSttToClient(ctx.userId, forcedResponse);
                        }

                        if (result.isAbuse() && track == CallTrack.INBOUND) { // INBOUND 트랙만 욕설 처리
                            log.info("STT 결과 욕설 감지 - (isAbuse={}) / CallSession total_abuse_cnt 업데이트 시도 - CallSessionId={}", result.isAbuse(), ctx.callSessionId);
                            callSessionService.incrementTotalAbuseCnt(ctx.callSessionId);
                        }

                        if (result.isAbuse() && track == CallTrack.INBOUND) {
                            callLogService.updateAbuse(ctx.callSession, track);
                        }
                    }

                } catch (Exception e) {
                    log.warn("❗ 강제 final 처리 실패", e);
                }
                ctx.partialFinalTranscript = null;
            }

            if (ctx.stream != null) {
                ctx.stream.closeSend();
            }

            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.MULAW)
                    .setSampleRateHertz(8000)
                    .setLanguageCode("ko-KR")
                    .build();

            StreamingRecognitionConfig streamingConfig = StreamingRecognitionConfig.newBuilder()
                    .setConfig(config)
                    .setInterimResults(true)
                    .build();

            ResponseObserver<StreamingRecognizeResponse> observer = createResponseObserver(ctx, track, sessionId);

            ctx.stream = ctx.client.streamingRecognizeCallable().splitCall(observer);
            ctx.stream.send(StreamingRecognizeRequest.newBuilder()
                    .setStreamingConfig(streamingConfig)
                    .build());

            ctx.lastStreamStartTime = System.currentTimeMillis();
            log.info("🔄 [{}] Google STT Stream 재시작됨", track);

        } catch (Exception e) {
            log.error("❌ STT 재시작 중 오류", e);
        }
    }

    private ResponseObserver<StreamingRecognizeResponse> createResponseObserver(STTContext ctx, CallTrack track, String sessionId) {
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
                            if (isFinal && track == CallTrack.INBOUND) {
                                String trimmedTranscript = transcript.trim();

                                // ✅ 중복 방지: 이전 저장값과 동일한 경우 저장 생략
                                if (trimmedTranscript.equals(ctx.lastSavedFinalTranscript)) {
                                    log.debug("⏭️ [중복 제거] 동일한 최종 텍스트 무시됨: {}", trimmedTranscript);
                                } else {
                                    var analysis = fastClient.sendTextToFastAPI(trimmedTranscript);

                                    CallSttLog savedLog = callSttLogService.saveTranscriptLog(
                                            ctx.callSessionId,
                                            track,
                                            trimmedTranscript,
                                            true,
                                            analysis.isAbuse(),
                                            analysis.getType()
                                    );

                                    // ✅ transcriptBuilder 중복 누적 방지
                                    if (!trimmedTranscript.equals(ctx.lastSavedFinalTranscript)) {
                                        ctx.transcriptBuilder.append(trimmedTranscript).append(" ");
                                        ctx.lastSavedFinalTranscript = trimmedTranscript;
                                    }

                                    CallSttLogResponseDTO finalResponse = new CallSttLogResponseDTO(DATA_TYPE_STT, savedLog);

                                    if (ctx.userId != null) {
                                        sttWebSocketHandler.sendSttToClient(ctx.userId, finalResponse);
                                    }

                                    if (analysis.isAbuse()) {
                                        // callLogService.registerAbuse(ctx.callSessionId, track);
                                        log.info("STT 결과 욕설 감지 - (isAbuse={}) / CallSession total_abuse_cnt 업데이트 시도 - CallSessionId={}", analysis.isAbuse(), ctx.callSessionId);
                                        callSessionService.incrementTotalAbuseCnt(ctx.callSessionId);
                                        log.info("🍀 고객 발화 필터링됨");
                                        callLogService.updateAbuse(ctx.callSession, track);
                                    }
                                }

                                ctx.partialFinalTranscript = null;

                            } else {
                                CallSttLog interimLog = CallSttLog.builder()
                                        .callSessionId(ctx.callSessionId)
                                        .track(track)
                                        .script(transcript)
                                        .isFinal(false)
                                        .isAbuse(false)
                                        .abuseType("정상")
                                        .abuseCnt(0)
                                        .build();

                                CallSttLogResponseDTO interimResponse = new CallSttLogResponseDTO(DATA_TYPE_STT, interimLog);

                                if (ctx.userId != null) {
                                    sttWebSocketHandler.sendSttToClient(ctx.userId, interimResponse);
                                }

                                ctx.partialFinalTranscript = transcript;
                            }

                        } catch (Exception e) {
                            log.error("❌ FastAPI 전송 오류", e);
                        }
                    }
                }
            }

            public void onError(Throwable t) {
                log.error("❌ [{}] STT 오류", track, t);
            }

            public void onComplete() {
                log.info("✅ [{}] STT 완료", track);
            }
        };
    }
}