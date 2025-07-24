package callprotector.spring.service.CallLogService;

import callprotector.spring.domain.CallSession;
import callprotector.spring.domain.enums.CallTrack;

public interface CallLogService {
    void saveFinalTranscript(Long callSessionId, CallTrack track, String script, boolean isAbuse, String abuseType);
    void updateAbuse(CallSession callSession, CallTrack track);
    String generateAiSummary(Long callSessionId);
}
