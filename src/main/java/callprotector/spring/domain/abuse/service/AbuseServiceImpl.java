package callprotector.spring.domain.abuse.service;

import callprotector.spring.domain.abuse.dto.response.AbuseResponseDTO;
import callprotector.spring.domain.callsession.entity.CallSession;
import callprotector.spring.global.client.FastClient;
import callprotector.spring.domain.abuse.entity.AbuseLog;
import callprotector.spring.domain.abuse.entity.AbuseType;
import callprotector.spring.domain.calllog.entity.CallLog;
import callprotector.spring.domain.abuse.entity.AbuseTypeLog;
import callprotector.spring.domain.abuse.repository.AbuseLogRepository;
import callprotector.spring.domain.abuse.repository.AbuseTypeLogRepository;
import callprotector.spring.domain.abuse.repository.AbuseTypeRepository;
import callprotector.spring.global.sms.service.SmsService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AbuseServiceImpl implements AbuseService{

    private final FastClient fastClient;

    private final AbuseLogRepository abuseLogRepository;
    private final AbuseTypeRepository abuseTypeRepository;
    private final AbuseTypeLogRepository abuseTypeLogRepository;

    private final SmsService smsService;

    @Override
    public AbuseResponseDTO.AbuseFilterDTO analyzeText(String text) {
        return fastClient.sendTextToFastAPI(text);
    }

    @Override
    @Transactional
    public void saveAbuseLogs(CallLog callLog, String abuseTypeStr) {
        // 1. AbuseLog 생성
        AbuseLog abuseLog = AbuseLog.builder()
                .callLog(callLog)
                .detectedAt(LocalDateTime.now())
                .build();
        abuseLogRepository.save(abuseLog);

        // 2. AbuseType 생성
        AbuseType abuseType = AbuseType.builder()
                .verbalAbuse(abuseTypeStr.contains("욕설") || abuseTypeStr.contains("욕설(강제차단)"))
                .sexualHarass(abuseTypeStr.contains("성희롱"))
                .threat(abuseTypeStr.contains("협박"))
                .build();
        abuseTypeRepository.save(abuseType);

        // 3. AbuseTypeLog 저장
        AbuseTypeLog typeLog = AbuseTypeLog.builder()
                .abuseLog(abuseLog)
                .abuseType(abuseType)
                .build();
        abuseTypeLogRepository.save(typeLog);

        log.info("🚨 Abuse 유형 로그 저장 완료: [{}] → 욕설: {}, 성희롱: {}, 협박: {}",
                abuseTypeStr,
                abuseType.isVerbalAbuse(),
                abuseType.isSexualHarass(),
                abuseType.isThreat()
        );

        try {
            CallSession session = callLog.getCallSession();

            if (session == null) {
                log.warn("❗ CallLog에 연결된 CallSession이 없습니다. callLogId={}", callLog.getId());
                return;
            }
            if (!session.isForcedTerminated()) {
                // 강제 종료가 아닌 일반 종료라면 전송 X
                return;
            }
            if (session.getTotalAbuseCnt() < 3) {
                // 폭언이 3회 이상인 경우만 전송
                return;
            }

            String to = session.getCallerNumber();

            if (to == null || to.isBlank()) {
                log.warn("❗ 고객 번호가 없어 종료 안내 SMS 발송을 생략합니다. sessionId={}", session.getId());
                return;
            }

            Set<String> labels = new LinkedHashSet<>();
            if (abuseTypeLogRepository
                    .existsByAbuseLog_CallLog_CallSession_IdAndAbuseType_VerbalAbuseTrue(session.getId())) {
                labels.add("욕설");
            }
            if (abuseTypeLogRepository
                    .existsByAbuseLog_CallLog_CallSession_IdAndAbuseType_SexualHarassTrue(session.getId())) {
                labels.add("성희롱");
            }
            if (abuseTypeLogRepository
                    .existsByAbuseLog_CallLog_CallSession_IdAndAbuseType_ThreatTrue(session.getId())) {
                labels.add("협박");
            }
            if (labels.isEmpty()) labels.add("부적절한 발언");

            smsService.sendTerminationNotice(to, labels);
            log.info("✅ 통화 강제 종료 사유 SMS 발송 완료 - to={}, types={}\"", to, labels);

        } catch (Exception e) {
            log.error("❌ 통화 강제 종료 사유 SMS 발송 실패 - callLogId={}, err={}", callLog.getId(), e.getMessage(), e);
        }

    }

}
