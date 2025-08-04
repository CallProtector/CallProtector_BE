package callprotector.spring.domain.chat.service;

import callprotector.spring.domain.chat.dto.response.ChatLogResponseDTO;

import java.util.List;

public interface ChatLogService {

    public List<ChatLogResponseDTO.ChatLogResponse> getChatLogsBySession(Long sessionId);

    // SSE 용
    public void saveChatLog(Long sessionId, String question, String answer, String sourcePages);
}
