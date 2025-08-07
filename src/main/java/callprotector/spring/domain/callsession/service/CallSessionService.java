package callprotector.spring.domain.callsession.service;

import callprotector.spring.domain.callsession.entity.CallSession;
import callprotector.spring.domain.callsession.dto.request.CallSessionRequestDTO;
import callprotector.spring.domain.callsession.dto.response.CallSessionResponseDTO;

public interface CallSessionService {
    CallSession getCallSession(Long callSessionId);

    void incrementTotalAbuseCnt(Long callSessionId);

    void forceTerminateCall(CallSession callSession);

    CallSessionResponseDTO.CallSessionPagingDTO getCallSessions(Long userId, String sortBy, String order, Long cursorId, int size);

    CallSessionResponseDTO.CallSessionPagingDTO getSessionsByAbuseCategory(Long userId, String category, Long cursorId, int size, String order);

    CallSessionResponseDTO.CallSessionPagingDTO searchCallSessions(Long userId, String keyword, String category, String order, Long cursorId, int size);

    CallSessionResponseDTO.CallSessionDetailResponseDTO getUserCallSessionDetail(Long callSessionId, Long userId);

    String generateSummaryByOpenAi(Long callSessionId, Long userId);

    CallSessionResponseDTO.CallSessionSummaryResponseDTO createCallSessionSummaryByOpenAi(Long callSessionId, Long userId);

    String generateSummaryByGemini(Long callSessionId, Long userId);

    CallSessionResponseDTO.CallSessionSummaryResponseDTO createCallSessionSummaryByGemini(Long callSessionId, Long userId);

    void updateEndedAt(Long callSessionId);

    CallSessionResponseDTO.CallSessionInfoDTO registerAcceptedUser(CallSessionRequestDTO.CallSessionMakeDTO dto, Long userId);

    CallSession findByCallSid(String callSid);

    Long createTempSession(Long userId, CallSessionRequestDTO.CallSessionMakeDTO dto);

    CallSessionResponseDTO.CallSessionInfoDTO getSessionInfo(Long callSessionId);

    CallSessionResponseDTO.AbusiveCallSessionPagingDTO getAbusiveCallSessions(Long userId, Long cursorId, int size);
}
