package callprotector.spring.domain.callchat.controller;

import callprotector.spring.domain.callchat.service.CallChatbotService;
import callprotector.spring.global.security.TokenProvider;
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
public class CallChatbotController {

    private final CallChatbotService chatbotService;
    private final TokenProvider tokenProvider;

    @GetMapping(value = "/analyze/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> analyzeSession(@PathVariable Long sessionId, @RequestParam String token) {
        // userId 추출
        Long userId = tokenProvider.validateAndGetUserId(token);

        return chatbotService.analyzeCallsession(sessionId, userId);
    }



}
