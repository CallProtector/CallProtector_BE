package callprotector.spring.domain.chat.controller;

import callprotector.spring.domain.chat.entity.ChatSession;
import callprotector.spring.domain.chat.service.ChatLogService;
import callprotector.spring.domain.chat.service.ChatSessionService;
import callprotector.spring.global.annotation.UserId;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatStreamController {

    private final WebClient webClient = WebClient.create("http://localhost:8000"); // FastAPI URL
    private final ChatLogService chatLogService;
    private final ChatSessionService chatSessionService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@UserId Long userId, @RequestParam Long sessionId, @RequestParam String question) {

        // 세션 소유권 검증
        ChatSession session = chatSessionService.getSessionById(sessionId);
        if (!session.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("해당 세션에 접근할 권한이 없습니다.");
        }


        StringBuilder jsonBuffer = new StringBuilder();

        return webClient.post()
                .uri("/stream")
                .bodyValue(Map.of("session_id", sessionId, "question", question))
                .retrieve()
                .bodyToFlux(String.class)
                .map(data -> data.replace("data:", "").trim())
                .doOnNext(chunk -> {
                    if (chunk.startsWith("[JSON]")) {
                        String jsonPart = chunk.replace("[JSON]", "").trim();
                        jsonBuffer.append(jsonPart);
                    }
                })
                .doOnComplete(() -> {
                    try {
                        if (jsonBuffer.length() > 0) {
                            log.info("📥 최종 JSON: {}", jsonBuffer);

                            // ✅ JSON 키 공백 정리
                            String normalizedJson = jsonBuffer.toString()
                                    .replaceAll("\"\\s*([^\"]*?)\\s*\"\\s*:", "\"$1\":")
                                    .replaceAll(":\\s*\"\\s*([^\"]*?)\\s*\"", ":\"$1\"");

                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode jsonNode = mapper.readTree(normalizedJson);

                            String answer = jsonNode.get("answer").asText();
                            log.info("💾 DB 저장 전 answer: {}", answer);

                            String sourcePages = mapper.writeValueAsString(jsonNode.get("sourcePages"));
                            chatLogService.saveChatLog(sessionId, question, answer, sourcePages);

                            // ✅ 첫 질문이면 세션 타이틀 생성
                            if (session.getTitle() == null || session.getTitle().isBlank()) {
                                chatSessionService.updateTitleIfEmpty(session, question);
                            }
                        }
                    } catch (Exception e) {
                        log.error("❌ JSON 파싱 오류", e);
                    }
                })
                .delayElements(Duration.ofMillis(20));
    }
}
