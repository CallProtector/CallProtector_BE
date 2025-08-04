package callprotector.spring.domain.calllog.service;

import callprotector.spring.domain.calllog.entity.CallLog;
import callprotector.spring.domain.callsession.entity.CallSession;
import callprotector.spring.domain.calllog.repository.CallLogRepository;
import callprotector.spring.global.common.enums.CallTrack;
import callprotector.spring.domain.abuse.service.AbuseService;
import callprotector.spring.domain.callsession.service.CallSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallLogServiceImpl implements CallLogService{

    private final CallLogRepository callLogRepository;
    private final CallSessionService callSessionService;
    private final AbuseService abuseService;

    @Override
    @Transactional
    public void saveFinalTranscript(final Long callSessionId, final CallTrack track, final String script, final boolean isAbuse, final String abuseType) {
        CallSession callSession = callSessionService.getCallSession(callSessionId);
        String audioUrl = track == CallTrack.INBOUND ? "customer.wav" : "agent.wav"; // 추후 변경 예정
        Integer sessionAbuseCnt = callSession.getTotalAbuseCnt();

        Integer callLogAbuseCnt = (track == CallTrack.INBOUND) ? sessionAbuseCnt : 0;

        CallLog callLog = callLogRepository.findByCallSessionAndTrack(callSession, track)
                .orElseGet(() -> CallLog.builder()
                        .callSession(callSession)
                        .audio_url(audioUrl)
                        .script(script)
                        .abuseCnt(callLogAbuseCnt)
                        .abuseDetect(isAbuse)
                        .track(track)
                        .build()
                );

        callLog.updateScript(script);

        callLogRepository.save(callLog);
        log.info("📌 CallLog 저장 완료: track = {}, isAbuse = {}, abuseType = {}", track, isAbuse, abuseType);

        // 인바운드 발화이면서 욕설이 감지된 경우 abuse 로그 저장
        if (isAbuse && track == CallTrack.INBOUND) {
            abuseService.saveAbuseLogs(callLog, abuseType);
        }
        log.info("callLog saved");
    }

    @Override
    @Transactional
    public void updateAbuse(Long callSessionId, CallTrack track) {
        if (track != CallTrack.INBOUND) return;

        // log.setAbuseCnt((log.getAbuseCnt() == null ? 0 : log.getAbuseCnt()) + 1);
        // log.setAbuseDetect(true);
        // callLogRepository.save(log);
        //
        // saveAbuseLogs(log);
    }

}
