package callprotector.spring.web.controller;

import callprotector.spring.apiPayload.ApiResponse;
import callprotector.spring.service.ChatbotService.ChatLogService;
import callprotector.spring.web.dto.request.ChatbotRequestDTO;
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

    @PostMapping("/ask")
    public ApiResponse<ChatLogResponseDTO.ChatLogResponse> askAndSaveLog(@RequestBody ChatbotRequestDTO.ChatbotRequest request) {
        log.info("✅ ChatLogController 진입: sessionId={}, question={}", request.getSessionId(), request.getQuestion());
        return ApiResponse.onSuccess(chatLogService.askAndSaveChatLog(request.getSessionId(), request.getQuestion()));
    }

    @GetMapping("/session/{sessionId}")
    public ApiResponse<List<ChatLogResponseDTO.ChatLogResponse>> getLogs(@PathVariable Long sessionId) {
        return ApiResponse.onSuccess(chatLogService.getChatLogsBySession(sessionId));
    }

}
