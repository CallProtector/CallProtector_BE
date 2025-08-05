package callprotector.spring.domain.abuse.service;

import callprotector.spring.domain.abuse.dto.response.AbuseFilterResponseDTO;
import callprotector.spring.global.client.FastClient;
import callprotector.spring.domain.abuse.entity.AbuseLog;
import callprotector.spring.domain.abuse.entity.AbuseType;
import callprotector.spring.domain.calllog.entity.CallLog;
import callprotector.spring.domain.abuse.entity.AbuseTypeLog;
import callprotector.spring.domain.abuse.repository.AbuseLogRepository;
import callprotector.spring.domain.abuse.repository.AbuseTypeLogRepository;
import callprotector.spring.domain.abuse.repository.AbuseTypeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AbuseServiceImpl implements AbuseService{

    private final FastClient fastClient;

    private final AbuseLogRepository abuseLogRepository;
    private final AbuseTypeRepository abuseTypeRepository;
    private final AbuseTypeLogRepository abuseTypeLogRepository;

    @Override
    public AbuseFilterResponseDTO analyzeText(String text) {
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
    }

}
