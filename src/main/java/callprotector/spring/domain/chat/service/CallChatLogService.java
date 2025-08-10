package callprotector.spring.domain.chat.service;

import callprotector.spring.domain.chat.dto.response.CallChatLogResponseDTO;
import callprotector.spring.domain.chat.entity.CallChatLog;

import java.util.List;

public interface CallChatLogService {

    public void saveCallChatLog(Long sessionId, String question, String answer, String sourcePages);
    public List<CallChatLogResponseDTO.CallChatLogResponse> getLogDtosBySession(Long sessionId);


}
