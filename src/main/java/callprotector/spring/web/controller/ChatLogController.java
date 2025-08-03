package callprotector.spring.web.controller;

import callprotector.spring.apiPayload.ApiResponse;
import callprotector.spring.service.ChatLogService.ChatLogService;
import callprotector.spring.web.dto.response.ChatLogResponseDTO;
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
