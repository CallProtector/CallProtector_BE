package callprotector.spring.service.ChatLogService;

import callprotector.spring.web.dto.response.ChatLogResponseDTO;

import java.util.List;

public interface ChatLogService {

    public List<ChatLogResponseDTO.ChatLogResponse> getChatLogsBySession(Long sessionId);

    // SSE 용
    public void saveChatLog(Long sessionId, String question, String answer, String sourcePages);
}
