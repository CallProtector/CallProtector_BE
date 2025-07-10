package callprotector.spring.service.CallSttLogService;

import callprotector.spring.domain.CallSttLog;
import callprotector.spring.domain.enums.CallTrack;

public interface CallSttLogService {
    CallSttLog saveTranscriptLog(Long callSessionId, CallTrack track, String script, boolean isFinal, boolean isAbuse, String abuseType);
    boolean hasAbuseInSession(Long callSessionId);
    String getAbuseTypesBySessionId(Long callSessionId);
}
