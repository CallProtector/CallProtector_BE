package callprotector.spring.domain.chat.controller;

import callprotector.spring.domain.chat.entity.ChatSession;
import callprotector.spring.domain.chat.service.ChatLogService;
import callprotector.spring.domain.chat.service.ChatSessionService;
import callprotector.spring.global.apiPayload.code.status.ErrorStatus;
import callprotector.spring.global.apiPayload.exception.handler.ChatGeneralException;
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
// ★ ServerSentEvent 사용
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
// ★ 제목 생성은 블로킹 가능성이 있으므로 boundedElastic로
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
    // ★ 반환 타입: Flux<ServerSentEvent<String>>
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(
            @Parameter(description = "대화가 기록될 ChatSession ID", required = true)
            @RequestParam Long sessionId,
            @Parameter(description = "질문 내용", required = true)
            @RequestParam String question,
            @Parameter(description = "JWT 토큰", required = true)
            @RequestParam String token) {

        String jwt = token;
        Long userId = tokenProvider.validateAndGetUserId(jwt);

        ChatSession session = chatSessionService.getSessionById(sessionId);
        if (!session.getUser().getId().equals(userId)) {
            throw new ChatGeneralException(ErrorStatus.CHAT_SESSION_FORBIDDEN);
        }

        StringBuilder jsonBuffer = new StringBuilder();
        final boolean shouldEmitTitle = (session.getTitle() == null || session.getTitle().isBlank());

        // ★ 1) 제목 이벤트: 먼저 보냄
        Mono<ServerSentEvent<String>> titleEvent = shouldEmitTitle
                ? Mono.fromCallable(() -> {
            String newTitle = chatSessionService.updateTitleIfEmpty(session, question);
            ObjectMapper mapper = new ObjectMapper();
            String payload = mapper.writeValueAsString(Map.of(
                    "sessionId", sessionId,
                    "title", newTitle
            ));
            return ServerSentEvent.<String>builder()
                    .event("title")
                    .data(payload)
                    .build();
        }).subscribeOn(Schedulers.boundedElastic())
                : Mono.empty();

        // ★ 2) AI 응답 스트림
        Flux<ServerSentEvent<String>> aiStream = chatbotClient.sendChatRequest(
                        "/ai/chat/stream",
                        Map.of("session_id", sessionId, "question", question)
                )
                .map(raw -> raw.replaceFirst("^data:\\s*", "").trim())
                .doOnNext(chunk -> {
                    if (chunk.startsWith("[JSON]")) {
                        String jsonPart = chunk.replace("[JSON]", "").trim();
                        jsonBuffer.append(jsonPart);
                    }
                })
                .doOnComplete(() -> {
                    try {
                        if (jsonBuffer.length() > 0) {
                            String normalizedJson = jsonBuffer.toString()
                                    .replaceAll("\"\\s*([^\"]*?)\\s*\"\\s*:", "\"$1\":")
                                    .replaceAll(":\\s*\"\\s*([^\"]*?)\\s*\"", ":\"$1\"");
                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode jsonNode = mapper.readTree(normalizedJson);
                            String answer = jsonNode.get("answer").asText();
                            String sourcePages = mapper.writeValueAsString(jsonNode.get("sourcePages"));
                            chatLogService.saveChatLog(sessionId, question, answer, sourcePages);
                        }
                    } catch (Exception e) {
                        log.error("❌ JSON 파싱 오류", e);
                        throw new ChatGeneralException(ErrorStatus.CHAT_LOG_PARSE_ERROR);
                    }
                })
                .map(chunk -> ServerSentEvent.<String>builder().data(chunk).build())
                .onErrorResume(ex -> {
                    log.error("❌ SSE 스트림 에러", ex);
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error").data("stream_failed").build());
                });

        // ★ 3) 순서 변경: 제목 먼저 → AI 스트림
        return Flux.concat(titleEvent, aiStream)
                .delayElements(Duration.ofMillis(5));
    }
}
