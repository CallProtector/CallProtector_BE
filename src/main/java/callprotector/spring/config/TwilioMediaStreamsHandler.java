package callprotector.spring.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.StreamController;
import com.google.cloud.speech.v1.*;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;


@Component
@Slf4j
public class TwilioMediaStreamsHandler extends AbstractWebSocketHandler {
    private SpeechClient speechClient;
    private ClientStream<StreamingRecognizeRequest> clientStream;
    private ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();


    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("🔗 Twilio 연결됨: {}", session.getId());

        try {
            speechClient = SpeechClient.create();

            RecognitionConfig recognitionConfig = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.MULAW)
                    .setSampleRateHertz(8000)
                    .setLanguageCode("ko-KR")
                    .build();

            StreamingRecognitionConfig streamingConfig = StreamingRecognitionConfig.newBuilder()
                    .setConfig(recognitionConfig)
                    .setInterimResults(true)
                    .build();

            ResponseObserver<StreamingRecognizeResponse> responseObserver = new ResponseObserver<>() {
                public void onStart(StreamController controller) {
                    log.info("🟢 STT 스트리밍 시작됨");

                }

                public void onResponse(StreamingRecognizeResponse response) {
                   log.info("⭐️ StreamingRecognizeResponse가 수신");
                    if (response == null) {
                        log.warn("❌ 응답 자체가 null임");
                        return;
                    }

                    List<StreamingRecognitionResult> results = response.getResultsList();
                    log.info("📦 응답 결과 수: {}", results.size());

                    for (StreamingRecognitionResult result : response.getResultsList()) {
                        if (result.getAlternativesCount() > 0) {
                            String transcript = result.getAlternatives(0).getTranscript();
                            boolean isFinal = result.getIsFinal();
                            log.info("📝 {} 인식 결과: {}", isFinal ? "최종" : "중간", transcript);
                        } else {
                            log.warn("⚠️ 결과는 있지만 대안이 없음");
                        }
                    }
                }
                public void onError(Throwable t) {
                    log.error("❗ STT 오류 발생", t);
                }

                public void onComplete() {
                    log.info("✅ STT 세션 종료");
                }
            };

            clientStream = speechClient.streamingRecognizeCallable().splitCall(responseObserver);
            clientStream.send(StreamingRecognizeRequest.newBuilder()
                    .setStreamingConfig(streamingConfig)
                    .build());

            if (clientStream == null) {
                log.error("❌ STT clientStream 초기화 실패");
            } else {
                log.info("✅ clientStream 초기화 완료");
            }

        } catch (IOException e) {
            log.error("❌ SpeechClient 초기화 실패: {}", e.getMessage());
            closeSessionWithError(session, "STT 서비스 초기화 실패");
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (clientStream == null) {
            log.error("❌ clientStream is null. 초기화되지 않음");
            return;
        }

        String payload = message.getPayload();
        log.info("📨 초기 메시지 수신 (text): {}", payload);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(payload);

        if (json.has("event") && "start".equals(json.get("event").asText())) {
            JsonNode mediaFormat = json.get("start").get("mediaFormat");
            String encoding = mediaFormat.get("encoding").asText();
            int sampleRate = mediaFormat.get("sampleRate").asInt();
            int channels = mediaFormat.get("channels").asInt();
            log.info("Twilio Start Event - Encoding: {}, SampleRate: {}, Channels: {}", encoding, sampleRate, channels);
        }

        if (json.has("event") && "media".equals(json.get("event").asText())) {
            String track = json.get("media").get("track").asText();
            if (!"inbound".equals(track)) return; //inbound만 처리 중

            String base64 = json.get("media").get("payload").asText();
            byte[] audioBytes = Base64.getDecoder().decode(base64);


            // 오디오 파일로 저장해서 실제 음성이 들어오는지 확인 -> 확인 완료 ✅
            String filename = "test_audio_inbound_" + session.getId() + ".mulaw";

            try (FileOutputStream fos = new FileOutputStream(filename, true)) {
                fos.write(audioBytes);
            } catch (Exception e) {
                log.error("❗오디오 저장 실패", e);
            }


            // 오디오 들어올 때마다 바로 전송 (160 bytes 단위)
            clientStream.send(StreamingRecognizeRequest.newBuilder()
                    .setAudioContent(ByteString.copyFrom(audioBytes))
                    .build());
            log.info("📤 오디오 전송 ({} bytes)", audioBytes.length);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("❗오류 발생: {}", exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("🔌 연결 종료: {}", session.getId());
        try {
            // 마지막 버퍼 처리
            if (audioBuffer.size() > 0 && clientStream != null) {
                byte[] lastChunk = audioBuffer.toByteArray();
                clientStream.send(StreamingRecognizeRequest.newBuilder()
                        .setAudioContent(ByteString.copyFrom(lastChunk))
                        .build());
                audioBuffer.reset();
                log.info("📤 종료 전 STT 최종 청크 전송 완료 ({} bytes)", lastChunk.length);
            }
            if (clientStream != null) {
                log.info("📤 종료 전 침묵 추가 전송");
                byte[] silence = new byte[8000];  // 1초 분량 침묵 (mu-law 8000Hz)
                clientStream.send(StreamingRecognizeRequest.newBuilder()
                        .setAudioContent(ByteString.copyFrom(silence))
                        .build());

                Thread.sleep(1500);  // STT 응답 대기 시간

                clientStream.closeSend();  // 세션 종료
                log.info("✅ STT 스트림 closeSend() 호출 완료");
            }

        } catch (Exception e) {
            log.error("❗ WebSocket 세션 종료 중 오류 발생", e);
        }
    }

    private void closeSessionWithError(WebSocketSession session, String reason) {
        try {
            session.close(CloseStatus.SERVER_ERROR.withReason(reason));
        } catch (IOException e) {
            log.error("❗세션 강제 종료 실패: {}", e.getMessage());
        }
    }
}
