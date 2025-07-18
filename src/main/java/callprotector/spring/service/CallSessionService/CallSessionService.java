package callprotector.spring.service.CallSessionService;

import callprotector.spring.domain.CallSession;
import callprotector.spring.web.dto.request.CallSessionRequestDTO;
import callprotector.spring.web.dto.response.CallSessionResponseDTO;

import java.util.List;

public interface CallSessionService {
    Long createCallSession(String email, CallSessionRequestDTO.CallSessionMakeDTO dto );
    CallSessionResponseDTO.CallSessionInfoDTO getCallSessionInfo(Long callSessionId);
    CallSession getCallSession(Long callSessionId);
    void incrementTotalAbuseCnt(Long callSessionId);
    void forceTerminateCall(CallSession callSession);

    List<CallSessionResponseDTO.CallSessionListDTO> getCallSessions(String sortBy, String order);
    CallSessionResponseDTO.CallSessionDetailResponseDTO getCallSessionDetail(Long callSessionId);
}
