package callprotector.spring.domain.callchat.controller;

import callprotector.spring.domain.callchat.dto.response.CallChatLogResponseDTO;
import callprotector.spring.domain.chat.entity.CallChatSession;
import callprotector.spring.domain.chat.service.CallChatLogService;
import callprotector.spring.domain.chat.service.CallChatSessionService;
import callprotector.spring.global.annotation.UserId;
import callprotector.spring.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/call-chat-log")
public class CallChatLogController {

    private final CallChatSessionService callChatSessionService;
    private final CallChatLogService callChatLogService;

    @GetMapping("/session/{sessionId}")
    public ApiResponse<List<CallChatLogResponseDTO.CallChatLogResponse>> getLogs(
            @UserId Long userId,
            @PathVariable Long sessionId
    ) {
        CallChatSession session = callChatSessionService.getSessionById(sessionId);
        if (!session.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("해당 세션에 접근할 권한이 없습니다.");
        }

        return ApiResponse.onSuccess(callChatLogService.getLogDtosBySession(sessionId));
    }


}
