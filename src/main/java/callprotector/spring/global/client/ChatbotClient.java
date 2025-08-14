package callprotector.spring.global.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Flux;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatbotClient {

    private final WebClient webClient;

    @Value("${chatbot.url}")
    private String chatbotUrl;

    public Flux<String> sendChatRequest(String endpoint, Map<String, Object> payload) {
        return webClient.post()
                .uri(chatbotUrl + endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(payload)
                .retrieve()
                .bodyToFlux(String.class);
    }

}
