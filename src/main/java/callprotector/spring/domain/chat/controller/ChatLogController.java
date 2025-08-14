package callprotector.spring.domain.chat.controller;

import callprotector.spring.domain.chat.entity.ChatSession;
import callprotector.spring.domain.chat.service.ChatSessionService;
import callprotector.spring.global.annotation.UserId;
import callprotector.spring.global.apiPayload.ApiResponse;
import callprotector.spring.domain.chat.service.ChatLogService;
import callprotector.spring.domain.chat.dto.response.ChatLogResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat-log")
@Tag(name = "ChatLog", description = "일반 챗봇 로그 관련 API")
public class ChatLogController {

    private final ChatLogService chatLogService;
    private final ChatSessionService chatSessionService;

    @Operation( summary = "일반 채팅 세션별 로그 조회 API", description = "ChatSession에 해당하는 ChatLog들을 조회합니다.")
    @GetMapping("/session/{sessionId}")
    public ApiResponse<List<ChatLogResponseDTO.ChatLogResponse>> getLogs(@UserId Long userId,
                                                                         @Parameter(description = "조회할 ChatSession ID")
                                                                         @PathVariable Long sessionId) {
        // 세션 소유권 검증
        ChatSession session = chatSessionService.getSessionById(sessionId);
        if (!session.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("해당 세션에 접근할 권한이 없습니다.");
        }
        return ApiResponse.onSuccess(chatLogService.getChatLogsBySession(sessionId));
    }

}
