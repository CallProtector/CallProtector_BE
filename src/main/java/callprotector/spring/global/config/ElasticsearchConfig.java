package callprotector.spring.global.config;

import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.elasticsearch.client.RestClient;

@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
@Configuration
@EnableConfigurationProperties(ElasticsearchConfig.ElasticsearchProperties.class)
@RequiredArgsConstructor
public class ElasticsearchConfig {

    private final ElasticsearchProperties properties;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        RestClient restClient = RestClient.builder(
                new HttpHost(properties.getHost(), properties.getPort(), "http")
        ).build();

        ElasticsearchTransport transport = new RestClientTransport(
                restClient,
                new JacksonJsonpMapper(objectMapper)
        );

        return new ElasticsearchClient(transport);
    }

    @Getter
    @Setter
    @ConfigurationProperties(prefix = "elasticsearch")
    public static class ElasticsearchProperties {
        private String host;
        private int port;
        private boolean enabled;
    }
}


