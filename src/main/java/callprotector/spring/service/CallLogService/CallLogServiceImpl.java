package callprotector.spring.service.CallLogService;

import callprotector.spring.domain.AbuseLog;
import callprotector.spring.domain.AbuseType;
import callprotector.spring.domain.CallLog;
import callprotector.spring.domain.CallSession;
import callprotector.spring.domain.enums.CallTrack;
import callprotector.spring.domain.mapping.AbuseTypeLog;
import callprotector.spring.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CallLogServiceImpl implements CallLogService{

    private final CallLogRepository callLogRepository;
    private final CallSessionRepository callSessionRepository;
    private final AbuseLogRepository abuseLogRepository;
    private final AbuseTypeRepository abuseTypeRepository;
    private final AbuseTypeLogRepository abuseTypeLogRepository;

    @Override
    @Transactional
    public void registerAbuse(Long callSessionId, CallTrack track) {
        if (track != CallTrack.INBOUND) return; // 상담원은 기록 X

        CallSession session = callSessionRepository.findById(callSessionId)
                .orElseThrow(() -> new IllegalArgumentException("CallSession not found: " + callSessionId));

        CallLog log = callLogRepository.findByCallSessionIdAndTrack(callSessionId, track)
                .orElseGet(() -> {
                    CallLog newLog = CallLog.builder()
                            .callSession(session)
                            .audio_url("customer.wav")
                            .script("")
                            .summary("자동 요약 예정")
                            .abuseCnt(0)
                            .abuseDetect(false)
                            .track(track)
                            .build();
                    return callLogRepository.save(newLog);
                });

        log.setAbuseCnt((log.getAbuseCnt() == null ? 0 : log.getAbuseCnt()) + 1);
        log.setAbuseDetect(true);
        callLogRepository.save(log);

        saveAbuseLogs(log);
    }

    @Override
    @Transactional
    public void saveFinalTranscript(Long callSessionId, CallTrack track, String script, boolean isAbuse, String abuseType) {
        CallSession session = callSessionRepository.findById(callSessionId)
                .orElseThrow(() -> new IllegalArgumentException("CallSession not found: " + callSessionId));

        String audioUrl = track == CallTrack.INBOUND ? "customer.wav" : "agent.wav";

        CallLog log = callLogRepository.findByCallSessionIdAndTrack(callSessionId, track)
                .orElseGet(() -> CallLog.builder()
                        .callSession(session)
                        .audio_url(audioUrl)
                        .script(script)
                        .summary("자동 요약 예정")
                        .abuseCnt(isAbuse && track == CallTrack.INBOUND ? 1 : 0)
                        .abuseDetect(isAbuse)
                        .track(track)
                        .build()
                );

        log.setScript(script);
        log.setSummary("자동 요약 예정");
        if (isAbuse) {
            log.setAbuseDetect(true);
        }

        callLogRepository.save(log);
    }

    private void saveAbuseLogs(CallLog log) {
        AbuseLog abuseLog = AbuseLog.builder()
                .callLog(log)
                .detectedAt(LocalDateTime.now())
                .build();
        abuseLogRepository.save(abuseLog);

        // AbuseType 정의 - 현재는 verbalAbuse만 Y
        AbuseType abuseType = AbuseType.builder()
                .verbalAbuse("Y")
                .sexualHarass("N")
                .threat("N")
                .build();
        abuseTypeRepository.save(abuseType);

        AbuseTypeLog typeLog = AbuseTypeLog.builder()
                .abuseLog(abuseLog)
                .abuseType(abuseType)
                .build();
        abuseTypeLogRepository.save(typeLog);
    }

}
