package callprotector.spring.service.CallLogService;

import callprotector.spring.domain.AbuseLog;
import callprotector.spring.domain.AbuseType;
import callprotector.spring.domain.CallLog;
import callprotector.spring.domain.CallSession;
import callprotector.spring.domain.enums.CallTrack;
import callprotector.spring.domain.mapping.AbuseTypeLog;
import callprotector.spring.repository.*;
import callprotector.spring.service.CallSessionService.CallSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallLogServiceImpl implements CallLogService{

    private final CallLogRepository callLogRepository;
    private final AbuseLogRepository abuseLogRepository;
    private final AbuseTypeRepository abuseTypeRepository;
    private final AbuseTypeLogRepository abuseTypeLogRepository;
    private final CallSessionService callSessionService;

    // @Override
    // @Transactional
    // public void registerAbuse(Long callSessionId, CallTrack track) {
    //     if (track != CallTrack.INBOUND) return; // 상담원은 기록 X
    //
    //     CallSession session = callSessionRepository.findById(callSessionId)
    //             .orElseThrow(() -> new IllegalArgumentException("CallSession not found: " + callSessionId));
    //
    //     CallLog log = callLogRepository.findByCallSessionAndTrack(callSessionId, track)
    //             .orElseGet(() -> {
    //                 CallLog newLog = CallLog.builder()
    //                         .callSession(session)
    //                         .audio_url("customer.wav")
    //                         .script("")
    //                         .summary("자동 요약 예정")
    //                         .abuseCnt(0)
    //                         .abuseDetect(false)
    //                         .track(track)
    //                         .build();
    //                 return callLogRepository.save(newLog);
    //             });
    //
    //     log.setAbuseCnt((log.getAbuseCnt() == null ? 0 : log.getAbuseCnt()) + 1);
    //     log.setAbuseDetect(true);
    //     callLogRepository.save(log);
    //
    //     saveAbuseLogs(log);
    // }

    @Override
    @Transactional
    public void saveFinalTranscript(final Long callSessionId, final CallTrack track, final String script, final boolean isAbuse, final String abuseType) {
        CallSession callSession = callSessionService.getCallSession(callSessionId);
        String audioUrl = track == CallTrack.INBOUND ? "customer.wav" : "agent.wav"; // 추후 변경 예정
        Integer sessionAbuseCnt = callSession.getTotalAbuseCnt();

        Integer callLogAbuseCnt;
        if (track == CallTrack.INBOUND) {
            callLogAbuseCnt = sessionAbuseCnt;
        } else {
            callLogAbuseCnt = 0;
        }

        CallLog callLog = callLogRepository.findByCallSessionAndTrack(callSession, track)
                .orElseGet(() -> CallLog.builder()
                        .callSession(callSession)
                        .audio_url(audioUrl)
                        .script(script)
                        .summary("AI 요약 예정") // default
                        .abuseCnt(callLogAbuseCnt)
                        .abuseDetect(isAbuse)
                        .track(track)
                        .build()
                );

         callLog.updateScript(script);
         callLog.updateSummary("자동 요약 예정"); // AI 상담 요약 결과

        callLogRepository.save(callLog);
        log.info("callLog saved");
    }

    @Override
    @Transactional
    public void updateAbuse(CallSession callSession, CallTrack track) {
        if (track != CallTrack.INBOUND) return;

        // log.setAbuseCnt((log.getAbuseCnt() == null ? 0 : log.getAbuseCnt()) + 1);
        // log.setAbuseDetect(true);
        // callLogRepository.save(log);
        //
        // saveAbuseLogs(log);
    }

    private void saveAbuseLogs(CallLog callLog) {
        AbuseLog abuseLog = AbuseLog.builder()
                .callLog(callLog)
                .detectedAt(LocalDateTime.now())
                .build();
        abuseLogRepository.save(abuseLog);

        // 설명 필요
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
