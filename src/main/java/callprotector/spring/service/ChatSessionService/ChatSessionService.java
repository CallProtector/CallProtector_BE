package callprotector.spring.service.ChatSessionService;

import callprotector.spring.domain.ChatSession;
import callprotector.spring.domain.User;
import callprotector.spring.web.dto.response.ChatSessionResponseDTO;


public interface ChatSessionService {

    public ChatSession getSessionById(Long sessionId);

    ChatSessionResponseDTO.ChatSessionResponse createSession(User user);


}
