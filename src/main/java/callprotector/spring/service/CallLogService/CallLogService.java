package callprotector.spring.service.CallLogService;

public interface CallLogService {

    public void saveFinalTranscript(Long callSessionId, String script, boolean isAbuse, String abuseType);
}
