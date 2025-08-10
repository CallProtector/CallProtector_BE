package callprotector.spring.domain.chat.controller;

import callprotector.spring.domain.chat.entity.CallChatSession;
import callprotector.spring.domain.chat.service.CallChatLogService;
import callprotector.spring.domain.chat.service.CallChatSessionService;
import callprotector.spring.global.security.TokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/call-chat")
public class CallChatStreamController {

    private final WebClient webClient = WebClient.create("http://localhost:8000"); // FastAPI
    private final CallChatLogService callChatLogService;
    private final CallChatSessionService callChatSessionService;
    private final TokenProvider tokenProvider;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamCallChat(
            @RequestParam Long callChatSessionId,
            @RequestParam String question,
            @RequestParam String token
    ) {
        // 1) JWT → userId
        Long userId = tokenProvider.validateAndGetUserId(token);

        // 2) 세션 소유권 검증
        CallChatSession session = callChatSessionService.getSessionById(callChatSessionId);
        if (!session.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("해당 상담 기반 세션에 접근할 권한이 없습니다.");
        }

        StringBuilder jsonBuffer = new StringBuilder();

        // 3) FastAPI 호출 (일반 대화용 /stream 재사용)
        return webClient.post()
                .uri("/stream")
                .contentType(MediaType.APPLICATION_JSON)                 // 추가
                .accept(MediaType.TEXT_EVENT_STREAM)                     // 추가
                .bodyValue(Map.of(
                        "session_id", callChatSessionId,   // 백엔드 메모리 키로 쓰고 싶으면 이 값 활용
                        "question", question,
                        "mode", "CALL" // (선택) 프롬프트 차별화 플래그
                ))
                .retrieve()
                .bodyToFlux(String.class)
                .map(raw -> {                                            // data: 조건부 제거
                    String s = raw == null ? "" : raw.trim();
                    if (s.startsWith("data:")) s = s.substring(5).trim();
                    return s;
                })
                .filter(s -> !s.isEmpty())
                .doOnNext(chunk -> {
                    if ("[END]".equals(chunk)) return;                  // 그대로 프론트로
                    if (chunk.startsWith("[ERROR]")) {                  // 에러 로그
                        log.error("SSE ERROR from FastAPI: {}", chunk);
                        return;
                    }
                    if (chunk.startsWith("[JSON]")) {                   // 버퍼 초기화 후 저장
                        jsonBuffer.setLength(0);
                        jsonBuffer.append(chunk.substring("[JSON]".length()).trim());
                    }
                })
                .doOnError(e -> log.error("SSE proxy error", e))        // 에러 핸들링
                .doOnComplete(() -> {
                    try {
                        if (jsonBuffer.length() == 0) return;           // JSON 없으면 스킵
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode node = mapper.readTree(jsonBuffer.toString());

                        String answer = node.has("answer") ? node.get("answer").asText() : "";
                        String sourcePages = (node.has("sourcePages") && !node.get("sourcePages").isNull())
                                ? node.get("sourcePages").toString()
                                : "[]";

                        callChatLogService.saveCallChatLog(
                                callChatSessionId,
                                question,
                                answer,
                                sourcePages
                        );
                    } catch (Exception e) {
                        log.error("❌ 상담별 채팅 저장 실패", e);
                    }
                })
                .delayElements(Duration.ofMillis(20));
    }
}
