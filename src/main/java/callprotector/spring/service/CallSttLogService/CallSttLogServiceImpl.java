package callprotector.spring.service.CallSttLogService;

import callprotector.spring.domain.CallSttLog;
import callprotector.spring.domain.enums.CallTrack;
import callprotector.spring.repository.CallSttLogRepository;
import callprotector.spring.service.CallSessionService.CallSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallSttLogServiceImpl implements CallSttLogService {
    private final CallSttLogRepository callSttLogRepository;
    private final CallSessionService callSessionService;

    @Override
    @Transactional
    public CallSttLog saveTranscriptLog(Long callSessionId, CallTrack track, String script, boolean isFinal, boolean isAbuse, String abuseType) {
        Integer abuseCnt = isAbuse ? 1 : 0;
        CallSttLog sttLog = CallSttLog.builder()
                .callSessionId(callSessionId)
                .track(track)
                .script(script)
                .isFinal(isFinal)
                .isAbuse(isAbuse)
                .abuseType(abuseType)
                .abuseCnt(abuseCnt)
                .build();

        CallSttLog savedSttLog = callSttLogRepository.save(sttLog);
        log.info("MongoDB - CallSttLog 저장 완료: id={}", savedSttLog.getId());

        // 폭언 감지 시 CallSession 객체의 totalAbuseCnt 증가
        if (isAbuse) {
            log.info("STT 결과 욕설 감지 - (isAbuse={}) / CallSession total_abuse_cnt 업데이트 시도 - CallSessionId={}", isAbuse, callSessionId);
            callSessionService.incrementTotalAbuseCnt(callSessionId);
        }

        return savedSttLog;
    }
}
