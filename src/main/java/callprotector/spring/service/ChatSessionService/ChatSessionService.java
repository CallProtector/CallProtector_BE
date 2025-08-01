package callprotector.spring.service.ChatSessionService;

import callprotector.spring.web.dto.response.ChatSessionResponseDTO;


public interface ChatSessionService {

    ChatSessionResponseDTO.ChatSessionResponse createSession(String email);

}
