package callprotector.spring.config;

import callprotector.spring.client.FastClient;import callprotector.spring.domain.CallSession;
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

            String twilioCallSid = json.path("start").path("callSid").asText();

            if (!userIdStr.isEmpty()) {
                log.info("🎯 Twilio start event에서 받은 userId: {}", userIdStr);
                Long userId = Long.parseLong(userIdStr);

                // callSession 객체 생성
                Long callSessionId = callSessionService.createCallSession(
                        "dlthdal07@gmail.com", // TODO: 사용자 이메일 동적 처리
                        new CallSessionRequestDTO.CallSessionMakeDTO(userId, "자동 세션", twilioCallSid)
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
            if (ctx.partialFinalTranscript != null && !ctx.partialFinalTranscript.trim().isEmpty()) {

                try {
                    var analysis = fastClient.sendTextToFastAPI(ctx.partialFinalTranscript);
                    CallSttLog savedLog = callSttLogService.saveTranscriptLog(
                            ctx.callSessionId, track, ctx.partialFinalTranscript,
                            true, analysis.isAbuse(), analysis.getType());
                    CallSttLogResponseDTO response = new CallSttLogResponseDTO(DATA_TYPE_STT, savedLog);
                    if (ctx.userId != null) sttWebSocketHandler.sendSttToClient(ctx.userId, response);
                    ctx.transcriptBuilder.append(ctx.partialFinalTranscript).append(" ");
                    ctx.partialFinalTranscript = null;
                    if (analysis.isAbuse() && track == CallTrack.INBOUND)
                        // callLogService.registerAbuse(ctx.callSessionId, track);
                        callLogService.updateAbuse(ctx.callSession, track);
                } catch (Exception e) {
                    log.warn("❗ 마지막 중간 텍스트 처리 실패", e);
                }
            }
            ctx.buffer.reset(); // ✅ 재시작 전 buffer 비우기
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

//                // chj - ⭐ CallSession 강제 생성
//                Long callSessionId = callSessionService.createCallSession(
//                        "dlthdal07@gmail.com", // TODO: 추후 사용자 이메일 동적으로 처리
//                        new CallSessionRequestDTO.CallSessionMakeDTO(0L, "자동 세션")
//                );
//                ctx.callSessionId = callSessionId;

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

                // 최종 텍스트 누락 방지
                if (ctx.partialFinalTranscript != null && !ctx.partialFinalTranscript.trim().isEmpty()) {
                    String fallback = ctx.partialFinalTranscript.trim();
                    log.info("💡 [종료시 처리] 누락 가능 중간 텍스트 처리: {}", fallback);
                    finalTranscript += " " + fallback;
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
                            String finalAbuseType = hasAbuseInSttLog ? "누적 감지" : inboundResult.getType();

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

            // ✅ [하이브리드 처리] 마지막 중간 텍스트 강제 처리
            if (ctx.partialFinalTranscript != null && !ctx.partialFinalTranscript.trim().isEmpty()) {
                String forcedFinal = ctx.partialFinalTranscript.trim();
                log.info("💡 [강제 final] 중간 결과를 final로 처리: {}", forcedFinal);
                try {
                    var result = fastClient.sendTextToFastAPI(forcedFinal);
                    CallSttLog savedLog = callSttLogService.saveTranscriptLog(
                            ctx.callSessionId,
                            track,
                            forcedFinal,
                            true,
                            result.isAbuse(),
                            result.getType()

                    );

                    ctx.transcriptBuilder.append(forcedFinal).append(" ");
                    CallSttLogResponseDTO forcedResponse = new CallSttLogResponseDTO(DATA_TYPE_STT, savedLog);
                    if (ctx.userId != null) {
                        sttWebSocketHandler.sendSttToClient(ctx.userId, forcedResponse);
                    }

                } catch (Exception e) {
                    log.warn("❗ 강제 final 처리 실패", e);
                }
                ctx.partialFinalTranscript = null;
            }
//            // transcriptBuilder 누락 방지 → 저장
//            String bufferedText = ctx.transcriptBuilder.toString().trim();
//            if (!bufferedText.isEmpty()) {
//                log.info("📝 [재시작 전] 임시 텍스트 저장: {}", bufferedText);
//
//                var result = fastClient.sendTextToFastAPI(bufferedText);
//                CallSttLog savedLog = callSttLogService.saveTranscriptLog(
//                        ctx.callSessionId,
//                        track,
//                        bufferedText,
//                        true, // 강제로 final 처리
//                        result.isAbuse(),
//                        result.getType(),
//                        0
//                );
//
//                // 📤 웹소켓 전송
//                if (ctx.userId != null) {
//                    sttWebSocketHandler.sendToClient(ctx.userId, savedLog);
//                } else {
//                    log.warn("❗ WebSocket 사용자 세션(userId) 없음 → 재시작 전 로그 전송 실패");
//                }
//
//                ctx.transcriptBuilder.setLength(0); // 누적 리셋
//            }

//            ctx.stream.closeSend();
//            ctx.client.shutdownNow();
//            ctx.client = SpeechClient.create();

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
                            if(isFinal &&  track == CallTrack.INBOUND) {
                                var analysis = fastClient.sendTextToFastAPI(transcript);

                                CallSttLog savedLog = callSttLogService.saveTranscriptLog(
                                        ctx.callSessionId,
                                        track,
                                        transcript,
                                        true,
                                        analysis.isAbuse(),
                                        analysis.getType()
                                        );

                                ctx.transcriptBuilder.append(transcript).append(" ");
                                CallSttLogResponseDTO finalResponse = new CallSttLogResponseDTO(DATA_TYPE_STT, savedLog);
                                if (ctx.userId != null) {
                                    sttWebSocketHandler.sendSttToClient(ctx.userId, finalResponse);
                                }

                                ctx.partialFinalTranscript = null;

                                if (analysis.isAbuse()) {
                                    // callLogService.registerAbuse(ctx.callSessionId, track);
                                    log.info("🍀 고객 발화 필터링됨");
                                    callLogService.updateAbuse(ctx.callSession, track);
                                }
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