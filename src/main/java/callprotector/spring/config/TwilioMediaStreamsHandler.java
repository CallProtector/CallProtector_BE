package callprotector.spring.config;

import callprotector.spring.client.FastClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.StreamController;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;

@Component
@Slf4j
public class TwilioMediaStreamsHandler extends AbstractWebSocketHandler {
    private SpeechClient speechClient;
    private ClientStream<StreamingRecognizeRequest> clientStream;
    private final ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
    private long lastSendTime = System.currentTimeMillis();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("☆ WebSocket 연결됨: {}", session.getId());

        try {
            speechClient = SpeechClient.create();

            RecognitionConfig recognitionConfig = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.MULAW)
                    .setSampleRateHertz(8000)
                    .setLanguageCode("ko-KR")
                    .setUseEnhanced(true)
                    .build();

            StreamingRecognitionConfig streamingConfig = StreamingRecognitionConfig.newBuilder()
                    .setConfig(recognitionConfig)
                    .setInterimResults(true)
                    .build();

            ResponseObserver<StreamingRecognizeResponse> responseObserver = new ResponseObserver<>() {
                public void onStart(StreamController controller) {
                    log.info("☆ STT 스트리밍 시작됨");
                }

                public void onResponse(StreamingRecognizeResponse response) {
                    for (StreamingRecognitionResult result : response.getResultsList()) {
                        if (result.getAlternativesCount() > 0) {
                            String transcript = result.getAlternatives(0).getTranscript();
                            boolean isFinal = result.getIsFinal();

                            if (isFinal) {
                                log.info("☆★☆ 최종 인식 결과: {}", transcript);
                            } else {
                                log.info("☆ 중간 인식 결과: {}", transcript);
                            }
                        }
                    }
                }

                public void onError(Throwable t) {
                    log.error("☆ STT 오류 발생", t);
                }

                public void onComplete() {
                    log.info("☆ STT 세션 종료");
                }
            };

            clientStream = speechClient.streamingRecognizeCallable().splitCall(responseObserver);
            clientStream.send(StreamingRecognizeRequest.newBuilder()
                    .setStreamingConfig(streamingConfig)
                    .build());

            log.info("☆ clientStream 초기화 완료");

        } catch (IOException e) {
            log.error("☆  SpeechClient 초기화 실패: {}", e.getMessage());
            closeSessionWithError(session, "STT 서비스 초기화 실패");
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (clientStream == null) {
            log.error("☆ clientStream is null. 초기화되지 않음");
            return;
        }

        String payload = message.getPayload();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(payload);

        if (json.has("event") && "media".equals(json.get("event").asText())) {
            String track = json.get("media").get("track").asText();
            if (!"inbound".equals(track)) return;

            String base64 = json.get("media").get("payload").asText();
            byte[] audioBytes = Base64.getDecoder().decode(base64);

//            // 디버깅용 오디오 저장
//            String filename = "debug_audio_" + session.getId() + ".mulaw";
//            try (FileOutputStream fos = new FileOutputStream(filename, true)) {
//                fos.write(audioBytes);
//            }

            // 오디오 200ms 버퍼링 후 일정 크기로 묶어 주기적으로 전송
            audioBuffer.write(audioBytes);
            long now = System.currentTimeMillis();
            if (now - lastSendTime >= 200) {
                byte[] chunk = audioBuffer.toByteArray();
                try {
                    clientStream.send(StreamingRecognizeRequest.newBuilder()
                            .setAudioContent(ByteString.copyFrom(chunk))
                            .build());
                    log.info("☆ 오디오 청크 전송 ({} bytes)", chunk.length);
                } catch (Exception e) {
                    log.error("☆ 오디오 전송 중 예외 발생: {}", e.getMessage());
                }
                audioBuffer.reset();
                lastSendTime = now;
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("☆ WebSocket 오류 발생: {}", exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("☆ 연결 종료: {}", session.getId());

        try {
            if (clientStream != null) {
                byte[] silence = new byte[8000]; // 1초 무음 전송
                clientStream.send(StreamingRecognizeRequest.newBuilder()
                        .setAudioContent(ByteString.copyFrom(silence))
                        .build());

                log.info("☆ 종료 전 침묵 오디오 전송 완료");
                clientStream.closeSend();
                log.info("☆ STT 스트림 closeSend() 호출 완료");
            }

            if (speechClient != null) {
                // 응답 처리 시간 대기
                // 1초 무음 전송 후 즉시 종료
                boolean terminated = speechClient.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS);
                if (!terminated) {
                    log.warn("☆ SpeechClient 종료 대기 시간 초과. 강제 종료합니다.");
                }

                speechClient.shutdownNow();
                log.info("☆ SpeechClient 종료 완료");
            }

        } catch (Exception e) {
            log.error("☆ 세션 종료 처리 중 오류", e);
        }
    }


    private void closeSessionWithError(WebSocketSession session, String reason) {
        try {
            session.close(CloseStatus.SERVER_ERROR.withReason(reason));
        } catch (IOException e) {
            log.error("☆ 세션 종료 실패", e);
        }
    }
}
