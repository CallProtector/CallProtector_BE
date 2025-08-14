package callprotector.spring.domain.chat.controller;

import callprotector.spring.global.annotation.UserId;
import callprotector.spring.global.apiPayload.ApiResponse;
import callprotector.spring.domain.user.entity.User;
import callprotector.spring.domain.chat.service.ChatSessionService;
import callprotector.spring.domain.user.service.UserService;
import callprotector.spring.domain.chat.dto.response.ChatSessionResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat-sessions")
@Tag(name = "ChatSession", description = "일반 챗봇 세션 관련 API")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;
    private final UserService userService;

    @Operation(summary = "일반 채팅 세션 생성 API", description = "일반 법률 상담용 챗봇 채팅 세션을 생성합니다.")
    @PostMapping
    public ApiResponse<ChatSessionResponseDTO.ChatSessionResponse> createSession(@UserId Long userId) {
        User user = userService.getUserById(userId);
        return ApiResponse.onSuccess(chatSessionService.createSession(user));
    }


    @Operation(summary = "일반 채팅 세션 목록 조회 API", description = "유저에 해당하는 일반 챗봇 채팅 세션을 조회합니다.")
    @GetMapping("/list")
    public ApiResponse<List<ChatSessionResponseDTO.ChatSessionResponse>> getSessionList(@UserId Long userId) {
        return ApiResponse.onSuccess(chatSessionService.getSessionList(userId));
    }

}
