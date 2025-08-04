package callprotector.spring.global.config;

import callprotector.spring.global.handler.SttWebSocketHandler;
import callprotector.spring.global.handler.TwilioMediaStreamsHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer {

    private final TwilioMediaStreamsHandler twilioMediaStreamsHandler;
    private final SttWebSocketHandler sttWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Twilio 미디어 스트림 처리용 WebSocket (Server <-> Twilio)
        registry.addHandler(twilioMediaStreamsHandler, "/ws/audio")
                .setAllowedOrigins("*"); // 실제 서비스에서는 도메인 제한 필요

        // STT 결과 전달용 WebSocket (Client <-> Server)
        registry.addHandler(sttWebSocketHandler, "/ws/stt")
                .setAllowedOrigins("*"); // 실제 서비스에서는 도메인 제한 필요

        log.info("STT 결과 전달용 WebSocket 핸들러 등록 완료");
    }


}
