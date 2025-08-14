package callprotector.spring.domain.callchat.controller;

import callprotector.spring.domain.callsession.entity.CallSession;
import callprotector.spring.domain.callsession.service.CallSessionService;
import callprotector.spring.domain.chat.dto.response.CallChatSessionResponseDTO;
import callprotector.spring.domain.chat.service.CallChatSessionService;
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

    // 기존에 있는 CallChatSession 기준으로 불러오기 (이어서 진행하기 위함)
    @GetMapping("/by-call-session")
    public ApiResponse<CallChatSessionResponseDTO.CallChatSessionResponse> getOrCreateByCallSession(
            @UserId Long userId,
            @RequestParam Long callSessionId
    ) {
        var user = userService.getUserById(userId);
        var callSession = callSessionService.getCallSession(callSessionId);

        var sess = callChatSessionService.getOrCreate(user, callSession);

        return ApiResponse.onSuccess(
                CallChatSessionResponseDTO.CallChatSessionResponse.builder()
                        .sessionId(sess.getId())
                        .title(sess.getTitle())
                        .createdAt(sess.getCreatedAt().toString())
                        .build()
        );
    }


    @GetMapping("/list")
    public ApiResponse<List<CallChatSessionResponseDTO.CallChatSessionResponse>> getSessionList(@UserId Long userId) {
        return ApiResponse.onSuccess(callChatSessionService.getSessionListDtoByUserId(userId));
    }





}
