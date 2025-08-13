package callprotector.spring.domain.callsttlog.service;

import callprotector.spring.domain.callsttlog.entity.CallSttLog;
import callprotector.spring.global.common.enums.CallTrack;
import callprotector.spring.domain.callsttlog.repository.CallSttLogRepository;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
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
    private final ElasticsearchClient elasticsearchClient;

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

        // Elasticsearch 인덱싱
        new Thread(() -> {
            try {
                elasticsearchClient.index(i -> i
                        .index("call_stt_log")
                        .id(savedSttLog.getId())
                        .document(savedSttLog)
                        .refresh(Refresh.WaitFor)
                );
                log.info("Elasticsearch - CallSttLog 인덱싱 완료: id={}", savedSttLog.getId());
            } catch (IOException e) {
                log.error("❌ Elasticsearch 인덱싱 실패 - id: {}", savedSttLog.getId(), e);
            }
        }).start();

        return savedSttLog;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAbuseInSession(Long callSessionId) {
        return callSttLogRepository.existsByCallSessionIdAndIsAbuseTrue(callSessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public String getAbuseTypesBySessionId(Long callSessionId) {
        List<CallSttLog> abusiveLogs = callSttLogRepository.findByCallSessionIdAndIsAbuseTrue(callSessionId);
        Set<String> uniqueTypes = abusiveLogs.stream()
                .map(CallSttLog::getAbuseType)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return String.join(",", uniqueTypes); // "욕설(강제차단),협박" 등
    }

    @Override
    @Transactional(readOnly = true)
    public List<CallSttLog> getAllBySessionId(Long callSessionId) {
        log.info("getAllBySessionId 호출: callSessionId={}, IS_FINAL={}", callSessionId, IS_FINAL);

        List<CallSttLog> sttList = callSttLogRepository.findByCallSessionIdAndIsFinalOrderByTimestampAsc(callSessionId, IS_FINAL);
        log.info("DB 조회 결과 (sttList) 크기: {}", sttList.size());
        // if (sttList.isEmpty()) {
        //     log.warn("STT 로그를 찾을 수 없습니다: callSessionId={}, isFinal={}", callSessionId, IS_FINAL);
        //     throw new CallSttLogNotFoundException();
        // }
        return sttList;
    }
}
