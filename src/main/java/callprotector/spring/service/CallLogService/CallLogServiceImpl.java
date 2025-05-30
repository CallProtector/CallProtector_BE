package callprotector.spring.service.CallLogService;

import callprotector.spring.domain.CallLog;
import callprotector.spring.repository.CallLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CallLogServiceImpl implements CallLogService{

    private final CallLogRepository callLogRepository;

    @Override
    public void saveFinalTranscript(Long callSessionId, String script, boolean isAbuse, String abuseType) {
        Optional<CallLog> optionalCallLog = callLogRepository.findByCallSession_Id(callSessionId);

        if (optionalCallLog.isPresent()) {
            CallLog callLog = optionalCallLog.get();
            callLog.setScript(script);
            callLog.setAbuseDetect(isAbuse);
            callLog.setAbuseCnt(isAbuse? 1:0);  // abuse count 계산 방식은 필요 시 변경
            // callLog.setAbuseType(abuseType); // abuseType 욕설만 하니까 일단 보류
            callLogRepository.save(callLog);
        } else {
            throw new IllegalArgumentException("CallLog not found for sessionId: " + callSessionId);
        }



    }
}
