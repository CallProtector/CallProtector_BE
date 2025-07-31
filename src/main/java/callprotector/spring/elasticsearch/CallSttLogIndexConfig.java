package callprotector.spring.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

// 임시 비활성화용 어노테이션 추가 
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
@Slf4j
@Component
@RequiredArgsConstructor
public class CallSttLogIndexConfig {

    private final ElasticsearchClient esClient;
    private static final String INDEX_NAME = "call_stt_log";

    private static final String INDEX_DEFINITION_JSON = """
    {
      "settings": {
        "analysis": {
          "tokenizer": {
            "nori_tokenizer_custom": {
              "type": "nori_tokenizer",
              "decompound_mode": "mixed"
            }
          },
          "analyzer": {
            "nori_mixed_analyzer": {
              "type": "custom",
              "tokenizer": "nori_tokenizer_custom"
            }
          }
        }
      },
      "mappings": {
        "properties": {
          "call_session_id": { "type": "long" },
          "track":           { "type": "keyword" },
          "script": {
            "type": "text",
            "analyzer": "nori_mixed_analyzer",
            "search_analyzer": "nori_mixed_analyzer"
          },
          "is_abuse":   { "type": "boolean" },
          "abuse_type": { "type": "keyword" },
          "created_at": { "type": "date" },
          "timestamp": {
            "type": "date",
            "format": "strict_date_optional_time||epoch_millis"
          }
        }
      }
    }
    """;

    @PostConstruct
    public void recreateIndex() throws IOException {
        boolean exists = esClient.indices().exists(e -> e.index(INDEX_NAME)).value();

        // 인덱스 수정 시 삭제 로직 - 주석 해제 후 사용
//        if (exists) {
//            esClient.indices().delete(d -> d.index(INDEX_NAME));
//            log.info("🗑️ 기존 call_stt_log 인덱스 삭제 완료");
//            exists = false;
//        }

        if (exists) {
            log.info("ℹ️ call_stt_log 인덱스가 이미 존재합니다. 생성 생략");
            return;
        }

        esClient.indices().create(c -> c
                .index(INDEX_NAME)
                .withJson(new ByteArrayInputStream(INDEX_DEFINITION_JSON.getBytes(StandardCharsets.UTF_8)))
        );

        log.info("✅ call_stt_log 인덱스 생성 완료");
    }
}
