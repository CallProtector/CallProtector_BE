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
                        .summary("AI 요약 예정") // default
                        .abuseCnt(callLogAbuseCnt)
                        .abuseDetect(isAbuse)
                        .track(track)
                        .build()
                );

        callLog.updateScript(script);
        callLog.updateSummary("자동 요약 예정"); // AI 상담 요약 결과

        callLogRepository.save(callLog);
        log.info("📌 CallLog 저장 완료: track = {}, isAbuse = {}, abuseType = {}", track, isAbuse, abuseType);

        // 인바운드 발화이면서 욕설이 감지된 경우 abuse 로그 저장
        if (isAbuse && track == CallTrack.INBOUND) {
            saveAbuseLogs(callLog, abuseType);
        }
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

    private void saveAbuseLogs(CallLog callLog, String abuseTypeStr) {
        // 1. AbuseLog 생성
        AbuseLog abuseLog = AbuseLog.builder()
                .callLog(callLog)
                .detectedAt(LocalDateTime.now())
                .build();
        abuseLogRepository.save(abuseLog);

        // 2. AbuseType 생성 (3가지 유형 각각 처리)
        AbuseType abuseType = AbuseType.builder()
                .verbalAbuse(abuseTypeStr.contains("욕설") ? "Y" : "N")
                .sexualHarass(abuseTypeStr.contains("성희롱") ? "Y" : "N")
                .threat(abuseTypeStr.contains("협박") ? "Y" : "N")
                .build();
        abuseTypeRepository.save(abuseType);

        // 3. AbuseTypeLog로 연관 관계 저장
        AbuseTypeLog typeLog = AbuseTypeLog.builder()
                .abuseLog(abuseLog)
                .abuseType(abuseType)
                .build();
        abuseTypeLogRepository.save(typeLog);

        log.info("🚨 Abuse 유형 로그 저장 완료: [{}] → 욕설: {}, 성희롱: {}, 협박: {}",
                abuseTypeStr,
                abuseType.getVerbalAbuse(),
                abuseType.getSexualHarass(),
                abuseType.getThreat()
        );
    }

}
