package callprotector.spring.config;

import callprotector.spring.client.FastClient;
import callprotector.spring.web.dto.response.AbuseResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.StreamController;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
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

    private static class STTContext {
        SpeechClient client;
        ClientStream<StreamingRecognizeRequest> stream;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        long lastSendTime = System.currentTimeMillis();
        StringBuilder transcriptBuilder = new StringBuilder();

        WebSocketSession session; // ✅ 프론트에 전송하려면 세션 저장 필요
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

        if (!json.has("event") || !"media".equals(json.get("event").asText())) return;

        String track = json.get("media").get("track").asText();
        String base64 = json.get("media").get("payload").asText();
        byte[] audio = Base64.getDecoder().decode(base64);

        STTContext ctx = getOrCreateContext(session.getId(), track, session);
        ctx.buffer.write(audio);
        ctx.session = session;

        long now = System.currentTimeMillis();
        if (now - ctx.lastSendTime >= 200) {
            byte[] chunk = ctx.buffer.toByteArray();
            ctx.stream.send(StreamingRecognizeRequest.newBuilder()
                    .setAudioContent(ByteString.copyFrom(chunk))
                    .build());
            ctx.buffer.reset();
            ctx.lastSendTime = now;
        }
    }

    private STTContext getOrCreateContext(String sessionId, String track, WebSocketSession session) throws IOException {
        Map<String, STTContext> targetMap = "inbound".equals(track) ? inboundMap : outboundMap;

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

                ResponseObserver<StreamingRecognizeResponse> observer = new ResponseObserver<>() {
                    public void onStart(StreamController controller) {
                        log.info("🎤 STT 시작: {} [{}]", sessionId, track);
                    }

                    public void onResponse(StreamingRecognizeResponse response) {
                        for (StreamingRecognitionResult result : response.getResultsList()) {
                            if (result.getAlternativesCount() > 0) {
                                String transcript = result.getAlternatives(0).getTranscript();
                                boolean isFinal = result.getIsFinal();
                                log.info("💬 [{}][{}] {}", isFinal ? "최종" : "중간", track.equals("inbound") ? "고객" : "상담원", transcript);

                                try {
                                    // 🧠 욕설 판별
                                    var analysis = fastClient.sendTextToFastAPI(transcript);

                                    // 📤 실시간 응답 전달
                                    String json = mapper.writeValueAsString(Map.of(
                                            "type", track,
                                            "text", transcript,
                                            "isFinal", isFinal,
                                            "abuse", analysis.isAbuse(),
                                            "abuseType", analysis.getType()
                                    ));

                                    ctx.session.sendMessage(new TextMessage(json));

                                    // ✅ 최종 결과만 누적 저장
                                    if (isFinal) {
                                        ctx.transcriptBuilder.append(transcript).append(" ");
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

                ctx.stream = ctx.client.streamingRecognizeCallable().splitCall(observer);
                ctx.stream.send(StreamingRecognizeRequest.newBuilder().setStreamingConfig(streamingConfig).build());

                return ctx;
            } catch (IOException e) {
                throw new RuntimeException("STTContext 생성 실패", e);
            }
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("✅ 연결 종료: {}", session.getId());

        closeContext(inboundMap.remove(session.getId()), "inbound");
        closeContext(outboundMap.remove(session.getId()), "outbound");
    }

    private void closeContext(STTContext ctx, String label) {
        if (ctx != null) {
            try {
                ctx.stream.closeSend();

                Thread.sleep(300);

                ctx.client.shutdown();
                if (!ctx.client.awaitTermination(1, TimeUnit.SECONDS)) {
                    ctx.client.shutdownNow();
                }

                log.info("✅ [{}] STT 스트림 종료 완료", label);

                String finalTranscript = ctx.transcriptBuilder.toString().trim();
                if (!finalTranscript.isEmpty()) {
                    log.info("📝 [{}] 전체 텍스트: {}", label.equals("inbound") ? "고객" : "상담원", finalTranscript);

                    try {
                        AbuseResponseDTO.AbuseFilterDTO result = fastClient.sendTextToFastAPI(finalTranscript);
                        log.info("⚠️ [{}] 욕설 탐지 결과 → isAbuse: {}, type: {}",
                                label.equals("inbound") ? "고객" : "상담원",
                                result.isAbuse(), result.getType());
                    } catch (Exception e) {
                        log.error("🚨 [{}] 욕설 분석 실패", label, e);
                    }
                }

            } catch (Exception e) {
                log.error("❌ [{}] STT 종료 중 오류", label, e);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("❌ WebSocket 오류: {}", exception.getMessage());
    }
}