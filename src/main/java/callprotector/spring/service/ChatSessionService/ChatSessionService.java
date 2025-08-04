package callprotector.spring.service.ChatSessionService;

import callprotector.spring.domain.ChatSession;
import callprotector.spring.domain.user.entity.User;
import callprotector.spring.web.dto.response.ChatSessionResponseDTO;


public interface ChatSessionService {

    public ChatSession getSessionById(Long sessionId);

    ChatSessionResponseDTO.ChatSessionResponse createSession(User user);


}
