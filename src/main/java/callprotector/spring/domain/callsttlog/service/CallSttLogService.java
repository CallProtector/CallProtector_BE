package callprotector.spring.domain.callsttlog.service;

import java.util.List;

import callprotector.spring.domain.callsttlog.entity.CallSttLog;
import callprotector.spring.global.common.enums.CallTrack;

public interface CallSttLogService {
    CallSttLog saveTranscriptLog(Long callSessionId, CallTrack track, String script, boolean isFinal, boolean isAbuse, String abuseType);
    boolean hasAbuseInSession(Long callSessionId);
    String getAbuseTypesBySessionId(Long callSessionId);
    List<CallSttLog> getAllBySessionId(Long callSessionId);
}
