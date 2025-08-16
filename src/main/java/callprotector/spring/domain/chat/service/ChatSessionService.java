package callprotector.spring.domain.chat.service;

import callprotector.spring.domain.chat.entity.ChatSession;
import callprotector.spring.domain.user.entity.User;
import callprotector.spring.domain.chat.dto.response.ChatSessionResponseDTO;

import java.util.List;


public interface ChatSessionService {

    ChatSession getSessionById(Long sessionId);

    ChatSessionResponseDTO.ChatSessionResponse createSession(User user);

    public String updateTitleIfEmpty(ChatSession session, String firstQuestion);

    public List<ChatSessionResponseDTO.ChatSessionResponse> getSessionList(Long userId);

}
