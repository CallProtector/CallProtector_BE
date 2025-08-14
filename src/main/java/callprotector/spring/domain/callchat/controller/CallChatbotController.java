package callprotector.spring.domain.callchat.controller;

import callprotector.spring.domain.callchat.service.CallChatbotService;
import callprotector.spring.global.security.TokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chatbot")
@Tag(name = "CallChatbot", description = "상담별 채팅 상담 내역 전송 관련 API")
public class CallChatbotController {

    private final CallChatbotService chatbotService;
    private final TokenProvider tokenProvider;

    @Operation(summary = "상담별 채팅 상담 내역 전송 API", description ="상담원이 불러온 상담 내역을 챗봇에게 전송하고 분석 결과를 받아옵니다.")
    @GetMapping(value = "/analyze/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> analyzeSession(
            @Parameter(description = "불러올 상담 내역 CallSession ID", required = true)
            @PathVariable Long sessionId,

            @Parameter(description = "JWT 토큰", required = true)
            @RequestParam String token) {

        // userId 추출
        Long userId = tokenProvider.validateAndGetUserId(token);
        return chatbotService.analyzeCallsession(sessionId, userId);
    }


}
