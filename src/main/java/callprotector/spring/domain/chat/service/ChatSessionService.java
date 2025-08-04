package callprotector.spring.domain.chat.service;

import callprotector.spring.domain.chat.entity.ChatSession;
import callprotector.spring.domain.user.entity.User;
import callprotector.spring.domain.chat.dto.response.ChatSessionResponseDTO;


public interface ChatSessionService {

    public ChatSession getSessionById(Long sessionId);

    ChatSessionResponseDTO.ChatSessionResponse createSession(User user);


}
