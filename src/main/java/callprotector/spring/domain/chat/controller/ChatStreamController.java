package callprotector.spring.domain.chat.controller;

import callprotector.spring.domain.chat.entity.ChatSession;
import callprotector.spring.domain.chat.service.ChatLogService;
import callprotector.spring.domain.chat.service.ChatSessionService;
import callprotector.spring.global.client.ChatbotClient;
import callprotector.spring.global.security.TokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
@Tag(name = "ChatStream", description = "일반 채팅 질문 전송 관련 API")
public class ChatStreamController {

    private final ChatbotClient chatbotClient;

    private final ChatLogService chatLogService;
    private final ChatSessionService chatSessionService;
    private final TokenProvider tokenProvider;

    @Operation(
            summary = "일반 채팅 질문 전송 API",
            description ="상담원이 입력한 일반 법률 질문을 챗봇에게 전송하고 응답을 받아옵니다."
    )
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @Parameter(description = "대화가 기록될 ChatSession ID", required = true)
            @RequestParam Long sessionId,

            @Parameter(description = "질문 내용", required = true)
            @RequestParam String question,

            @Parameter(description = "JWT 토큰", required = true)
            @RequestParam String token) {

        // JWT 추출 (쿼리로만 받음)
        String jwt = token;
        log.info("🔑 전달된 JWT (query): {}", jwt);

        // userId 추출
        Long userId = tokenProvider.validateAndGetUserId(jwt);

        // 세션 소유권 검증
        ChatSession session = chatSessionService.getSessionById(sessionId);
        if (!session.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("해당 세션에 접근할 권한이 없습니다.");
        }

        StringBuilder jsonBuffer = new StringBuilder();

        return chatbotClient.sendChatRequest(
                        "/ai/chat/stream",
                        Map.of("session_id", sessionId, "question", question)
                )
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

                            // JSON 키 공백 정리
                            String normalizedJson = jsonBuffer.toString()
                                    .replaceAll("\"\\s*([^\"]*?)\\s*\"\\s*:", "\"$1\":")
                                    .replaceAll(":\\s*\"\\s*([^\"]*?)\\s*\"", ":\"$1\"");

                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode jsonNode = mapper.readTree(normalizedJson);

                            String answer = jsonNode.get("answer").asText();
                            log.info("💾 DB 저장 전 answer: {}", answer);

                            String sourcePages = mapper.writeValueAsString(jsonNode.get("sourcePages"));
                            chatLogService.saveChatLog(sessionId, question, answer, sourcePages);

                            // 첫 질문이면 세션 타이틀 생성
                            if (session.getTitle() == null || session.getTitle().isBlank()) {
                                chatSessionService.updateTitleIfEmpty(session, question);
                            }
                        }
                    } catch (Exception e) {
                        log.error("❌ JSON 파싱 오류", e);
                    }
                })
                .delayElements(Duration.ofMillis(5));
    }
}