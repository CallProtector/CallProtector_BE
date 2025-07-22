package callprotector.spring.service.CallSttLogService;

import callprotector.spring.apiPayload.exception.handler.CallSttLogNotFoundException;
import callprotector.spring.domain.CallSttLog;
import callprotector.spring.domain.enums.CallTrack;
import callprotector.spring.repository.CallSttLogRepository;
import callprotector.spring.service.CallSessionService.CallSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallSttLogServiceImpl implements CallSttLogService {
    private static final boolean IS_FINAL = true;
    private final CallSttLogRepository callSttLogRepository;

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
        // if (isAbuse) {
        //     log.info("STT 결과 욕설 감지 - (isAbuse={}) / CallSession total_abuse_cnt 업데이트 시도 - CallSessionId={}", isAbuse, callSessionId);
        //     callSessionService.incrementTotalAbuseCnt(callSessionId);
        // }

        return savedSttLog;
    }

    @Override
    public boolean hasAbuseInSession(Long callSessionId) {
        return callSttLogRepository.existsByCallSessionIdAndIsAbuseTrue(callSessionId);
    }

    @Override
    public String getAbuseTypesBySessionId(Long callSessionId) {
        List<CallSttLog> abusiveLogs = callSttLogRepository.findByCallSessionIdAndIsAbuseTrue(callSessionId);
        Set<String> uniqueTypes = abusiveLogs.stream()
                .map(CallSttLog::getAbuseType)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return String.join(",", uniqueTypes); // "욕설(강제차단),협박" 등
    }

    @Override
    public List<CallSttLog> getAllBySessionId(Long callSessionId) {
        List<CallSttLog> sttList = callSttLogRepository.findByCallSessionIdAndIsFinalOrderByTimestampAsc(callSessionId, IS_FINAL);
        if (sttList.isEmpty()) {
            throw new CallSttLogNotFoundException();
        }
        return sttList;
    }
}
