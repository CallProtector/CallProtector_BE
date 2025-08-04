package callprotector.spring.domain.callsttlog.repository;

import callprotector.spring.domain.callsttlog.entity.CallSttLog;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CallSttLogSearchRepositoryImpl implements CallSttLogSearchRepository {

    private final ElasticsearchClient esClient;
    private static final String INDEX_NAME = "call_stt_log";

    @Override
    public List<CallSttLog> searchByKeywordAndFilters(String keyword, String category, String order, Long cursorId, int size) {
        try {
            List<Query> mustQueries = new ArrayList<>();

            // keyword full-text 검색 (script)
            if (keyword != null && !keyword.isBlank()) {
                mustQueries.add(MatchQuery.of(m -> m
                        .field("script")
                        .query(keyword)
                )._toQuery());
            }

            // 카테고리 필터
            if (category != null && !category.isBlank()) {
                mustQueries.add(TermQuery.of(t -> t
                        .field("abuse_type")
                        .value(category)
                )._toQuery());
            }

            // 커서 기반 페이징
            if (cursorId != null) {
                mustQueries.add(RangeQuery.of(r -> r
                        .field("call_session_id")
                        .gt(JsonData.of(cursorId))
                )._toQuery());
            }

            // 정렬
            boolean isDesc = order == null || order.equalsIgnoreCase("desc");

            log.info("🔍 Elasticsearch 검색 keyword='{}', category='{}', order='{}', cursorId={}, size={}", keyword, category, order, cursorId, size);

            SearchRequest request = SearchRequest.of(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q
                            .bool(b -> b
                                    .must(mustQueries)
                            )
                    )
                    .sort(sort -> sort
                            .field(f -> f
                                    .field("created_at")
                                    .order(isDesc ? SortOrder.Desc : SortOrder.Asc)
                            )
                    )
                    .size(size)
            );

            SearchResponse<CallSttLog> response = esClient.search(request, CallSttLog.class);
            List<CallSttLog> results = new ArrayList<>();
            for (Hit<CallSttLog> hit : response.hits().hits()) {
                results.add(hit.source());
            }

            return results;

        } catch (IOException e) {
            log.error("Elasticsearch 검색 중 오류 발생", e);
            return List.of();
        }
    }
}