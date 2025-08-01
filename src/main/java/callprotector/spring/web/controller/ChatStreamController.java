package callprotector.spring.web.controller;

import callprotector.spring.service.ChatLogService.ChatLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;


import java.time.Duration;
import java.util.Map;

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
                .uri("/stream") // FastAPI 스트리밍 엔드포인트
                .bodyValue(Map.of("session_id", sessionId, "question", question))
                .retrieve()
                .bodyToFlux(String.class)
                .map(data -> data.replace("data:", "").trim()) // SSE 데이터 포맷 정리
                .doOnNext(fullAnswer::append) // SSE 수신 중 응답 누적
                .doOnComplete(() -> {
                    // SSE 종료 후 DB 저장
                    chatLogService.saveChatLog(sessionId, question, fullAnswer.toString());
                })
                .delayElements(Duration.ofMillis(20));
    }
}
