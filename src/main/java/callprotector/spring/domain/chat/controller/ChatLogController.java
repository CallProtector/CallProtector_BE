package callprotector.spring.domain.chat.controller;

import callprotector.spring.global.apiPayload.ApiResponse;
import callprotector.spring.domain.chat.service.ChatLogService;
import callprotector.spring.domain.chat.dto.response.ChatLogResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat-log")
public class ChatLogController {

    private final ChatLogService chatLogService;


    @GetMapping("/session/{sessionId}")
    public ApiResponse<List<ChatLogResponseDTO.ChatLogResponse>> getLogs(@PathVariable Long sessionId) {
        return ApiResponse.onSuccess(chatLogService.getChatLogsBySession(sessionId));
    }

}
