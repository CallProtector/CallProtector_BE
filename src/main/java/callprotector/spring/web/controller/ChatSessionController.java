package callprotector.spring.web.controller;

import callprotector.spring.global.annotation.UserId;
import callprotector.spring.apiPayload.ApiResponse;
import callprotector.spring.domain.User;
import callprotector.spring.service.ChatSessionService.ChatSessionService;
import callprotector.spring.service.UserService.UserService;
import callprotector.spring.web.dto.response.ChatSessionResponseDTO;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat-session")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;
    private final UserService userService;

    @PostMapping
    public ApiResponse<ChatSessionResponseDTO.ChatSessionResponse> createSession(
        @UserId Long userId
    ) {
        User user = userService.getUserById(userId);
        return ApiResponse.onSuccess(chatSessionService.createSession(user));
    }
}
