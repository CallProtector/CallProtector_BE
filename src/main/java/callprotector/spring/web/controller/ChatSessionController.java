package callprotector.spring.web.controller;

import callprotector.spring.apiPayload.ApiResponse;
import callprotector.spring.domain.User;
import callprotector.spring.service.ChatSessionService.ChatSessionService;
import callprotector.spring.service.UserService.UserService;
import callprotector.spring.web.dto.response.ChatSessionResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat-session")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;
    private final UserService userService;

    @PostMapping
    public ApiResponse<ChatSessionResponseDTO.ChatSessionResponse> createSession(Principal principal) {
        String email = principal.getName();  // JWT에서 추출된 email 값
        User user = userService.getUserByEmail(email); // UserService에서 조회
        return ApiResponse.onSuccess(chatSessionService.createSession(user));
    }

}
