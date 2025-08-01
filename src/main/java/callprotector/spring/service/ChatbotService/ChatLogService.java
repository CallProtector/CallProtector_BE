package callprotector.spring.service.ChatbotService;

import callprotector.spring.web.dto.response.ChatLogResponseDTO;

import java.util.List;

public interface ChatLogService {

    public ChatLogResponseDTO.ChatLogResponse askAndSaveChatLog(Long sessionId, String question);

    public List<ChatLogResponseDTO.ChatLogResponse> getChatLogsBySession(Long sessionId);

    // SSE 용
    public void saveChatLog(Long sessionId, String question, String answer);
}
