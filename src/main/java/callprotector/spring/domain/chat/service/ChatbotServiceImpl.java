package callprotector.spring.domain.chat.service;

import callprotector.spring.domain.chat.dto.request.SessionScriptHistoryRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotServiceImpl implements ChatbotService{

    private final WebClient.Builder webClientBuilder;

    @Override
    public Flux<ServerSentEvent<String>> analyzeCallsession(Long sessionId, List<SessionScriptHistoryRequestDTO.ScriptHistoryDTO> scriptHistory) {
        // FastAPI 요청 바디 생성
        Map<String, Object> requestBody = Map.of(
                "sessionId", sessionId,
                "scripts", scriptHistory.stream()
                        .map(script -> Map.of(
                                "speaker", script.getSpeaker(),
                                "text", script.getText()
                        ))
                        .toList()
        );
        // WebClient로 FastAPI SSE 호출
        return webClientBuilder.build()
                .post()
                .uri("http://fastapi-server:8000/api/chatbot/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class) // SSE 응답을 Flux로 변환
                .map(data -> ServerSentEvent.builder(data).build());
    }
}
