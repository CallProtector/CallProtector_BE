package callprotector.spring.domain.chat.controller;

import callprotector.spring.global.annotation.UserId;
import callprotector.spring.global.apiPayload.ApiResponse;
import callprotector.spring.domain.user.entity.User;
import callprotector.spring.domain.chat.service.ChatSessionService;
import callprotector.spring.domain.user.service.UserService;
import callprotector.spring.domain.chat.dto.response.ChatSessionResponseDTO;

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
