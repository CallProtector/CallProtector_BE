package callprotector.spring.elasticsearch;

import callprotector.spring.domain.CallSttLog;
import callprotector.spring.repository.CallSttLogRepository;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class CallSttLogIndexer {

    private final CallSttLogRepository callSttLogRepository;
    private final ElasticsearchClient esClient;

    @EventListener(ApplicationReadyEvent.class) // 애플리케이션 시작 시 실행
    public void reindexAllLogs() throws IOException {
        List<CallSttLog> mongoLogs = callSttLogRepository.findAll();

        log.info("📦 MongoDB에서 불러온 CallSttLog 개수: {}", mongoLogs.size());

        List<BulkOperation> operations = mongoLogs.stream()
                .map(log -> BulkOperation.of(b -> b
                        .index(i -> i
                                .index("call_stt_log")
                                .document(log)
                        )))
                .toList();

        BulkRequest bulkRequest = BulkRequest.of(b -> b.operations(operations));
        BulkResponse response = esClient.bulk(bulkRequest);

        if (response.errors()) {
            log.error("❌ 일부 문서 이관 실패: {}",
                    response.items().stream()
                            .filter(item -> item.error() != null)
                            .toList()
            );
        } else {
            log.info("🚀 Elasticsearch 재색인 완료: {}건", mongoLogs.size());
        }
    }
}
