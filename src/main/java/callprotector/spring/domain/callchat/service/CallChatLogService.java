package callprotector.spring.domain.callchat.service;

import callprotector.spring.domain.callchat.dto.response.CallChatLogResponseDTO;

import java.util.List;

public interface CallChatLogService {

    public void saveCallChatLog(Long sessionId, String question, String answer, String sourcePages);
    public List<CallChatLogResponseDTO.CallChatLogResponse> getLogDtosBySession(Long sessionId);


}
