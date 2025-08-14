package callprotector.spring.domain.callchat.controller;

import callprotector.spring.domain.callsession.entity.CallSession;
import callprotector.spring.domain.callsession.service.CallSessionService;
import callprotector.spring.domain.callchat.dto.response.CallChatSessionResponseDTO;
import callprotector.spring.domain.callchat.service.CallChatSessionService;
import callprotector.spring.domain.user.entity.User;
import callprotector.spring.domain.user.service.UserService;
import callprotector.spring.global.annotation.UserId;
import callprotector.spring.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/call-chat-sessions")
public class CallChatSessionController {

    private final CallChatSessionService callChatSessionService;
    private final UserService userService;
    private final CallSessionService callSessionService;

    @PostMapping
    public ApiResponse<CallChatSessionResponseDTO.CallChatSessionResponse> createSession(
            @UserId Long userId,
            @RequestParam Long callSessionId) {

        User user = userService.getUserById(userId);
        CallSession callSession = callSessionService.getCallSession(callSessionId);

        return ApiResponse.onSuccess(callChatSessionService.createCallChatSession(user, callSession));
    }



    @GetMapping("/list")
    public ApiResponse<List<CallChatSessionResponseDTO.CallChatSessionResponse>> getSessionList(@UserId Long userId) {
        return ApiResponse.onSuccess(callChatSessionService.getSessionListDtoByUserId(userId));
    }





}
