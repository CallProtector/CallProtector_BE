package callprotector.spring.domain.chat.service;

import callprotector.spring.domain.callsession.entity.CallSession;
import callprotector.spring.domain.chat.dto.response.CallChatSessionResponseDTO;
import callprotector.spring.domain.chat.entity.CallChatSession;
import callprotector.spring.domain.user.entity.User;

import java.util.List;

public interface CallChatSessionService {
    public CallChatSession getOrCreate(User user, CallSession callSession);
    public CallChatSessionResponseDTO.CallChatSessionResponse createCallChatSession(User user, CallSession callSession);
    public CallChatSession getSessionById(Long sessionId);
    public List<CallChatSessionResponseDTO.CallChatSessionResponse> getSessionListDtoByUserId(Long userId);
}
