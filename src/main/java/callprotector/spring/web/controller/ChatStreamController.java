package callprotector.spring.web.controller;

import callprotector.spring.service.ChatLogService.ChatLogService;
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

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestParam Long sessionId, @RequestParam String question) {

        StringBuilder fullAnswer = new StringBuilder();

        return webClient.post()
                .uri("/stream")
                .bodyValue(Map.of("session_id", sessionId, "question", question))
                .retrieve()
                .bodyToFlux(String.class)
                .map(data -> data.replace("data:", ""))
                .doOnNext(fullAnswer::append)
                .doOnComplete(() -> {
                    try {
                        String cleaned = fullAnswer.toString()
                                .replace("```json", "")
                                .replace("```", "")
                                .replace("data:", "")
                                .replace("[END]", "")
                                .trim();

                        int firstBraceIndex = cleaned.indexOf("{");
                        int lastBraceIndex = cleaned.lastIndexOf("}");
                        if (firstBraceIndex == -1 || lastBraceIndex == -1) {
                            log.error("❌ JSON 추출 실패: {}", cleaned);
                            return;
                        }
                        String jsonString = cleaned.substring(firstBraceIndex, lastBraceIndex + 1);

                        log.info("📥 FastAPI 응답 (정제 후): {}", jsonString);

                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode jsonNode = mapper.readTree(jsonString);

                        String answer = jsonNode.get("answer").asText();
                        String sourcePages = mapper.writeValueAsString(jsonNode.get("sourcePages"));

                        chatLogService.saveChatLog(sessionId, question, answer, sourcePages);
                    } catch (Exception e) {
                        log.error("❌ JSON 파싱 오류", e);
                    }
                })
                .delayElements(Duration.ofMillis(20));
    }
}
