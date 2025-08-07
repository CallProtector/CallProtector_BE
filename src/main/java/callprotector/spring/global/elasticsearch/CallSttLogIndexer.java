package callprotector.spring.global.elasticsearch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import callprotector.spring.domain.callsttlog.entity.CallSttLog;
import callprotector.spring.domain.callsttlog.repository.CallSttLogRepository;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import co.elastic.clients.elasticsearch.core.BulkRequest;

import java.io.IOException;
import java.util.List;

@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
@Slf4j
@Component
@RequiredArgsConstructor
public class CallSttLogIndexer {

    private final CallSttLogRepository callSttLogRepository;
    private final ElasticsearchClient esClient;
    private static final String INDEX_NAME = "call_stt_log";

    @EventListener(ApplicationReadyEvent.class)
    public void reindexAllLogs() throws IOException {
        // 색인된 문서가 존재하면 재색인 생략
        long docCount = esClient.search(s -> s
                .index(INDEX_NAME)
                .size(0), Void.class
        ).hits().total().value();

        if (docCount > 0) {
            log.info("ℹ️ call_stt_log 인덱스에 이미 {}건의 문서가 존재합니다. 재색인 생략", docCount);
            return;
        }

        // 색인된 문서가 없을 때만 재색인 진행
        List<CallSttLog> mongoLogs = callSttLogRepository.findAll();
        log.info("📦 MongoDB에서 불러온 CallSttLog 개수: {}", mongoLogs.size());

        List<BulkOperation> operations = mongoLogs.stream()
                .map(log -> BulkOperation.of(b -> b
                        .index(i -> i
                                .index(INDEX_NAME)
                                .document(log)
                        )))
                .toList();

        BulkRequest bulkRequest = BulkRequest.of(b -> b.operations(operations));
        BulkResponse response = esClient.bulk(bulkRequest);

        if (response.errors()) {
            log.error("❌ 일부 문서 이관 실패: {}", response.items().stream()
                    .filter(item -> item.error() != null)
                    .toList());
        } else {
            log.info("🚀 Elasticsearch 재색인 완료: {}건", mongoLogs.size());
        }
    }
}