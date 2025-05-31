package callprotector.spring.service.CallLogService;

import callprotector.spring.domain.CallLog;
import callprotector.spring.domain.CallSession;
import callprotector.spring.repository.CallLogRepository;
import callprotector.spring.repository.CallSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CallLogServiceImpl implements CallLogService{

    private final CallLogRepository callLogRepository;
    private final CallSessionRepository callSessionRepository;

    @Override
    public void saveFinalTranscript(Long callSessionId, String label, String script, boolean isAbuse, String abuseType) {
        CallSession callSession = callSessionRepository.findById(callSessionId)
                .orElseThrow(() -> new IllegalArgumentException("CallSession not found: " + callSessionId));

        String audioUrl = label.equals("inbound") ? "customer.wav" : "agent.wav";

        CallLog newLog = CallLog.builder()
                .callSession(callSession)
                .audio_url(audioUrl)
                .summary("자동 요약 예정")
                .script(script)
                .abuseDetect(isAbuse)
                .abuseCnt(isAbuse ? 1 : 0)
                .build();

        callLogRepository.save(newLog);
    }

}
