package callprotector.spring.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Configuration
@Getter
public class OpenAiConfig {

    @Value("${openai.secret-key}")
    private String secretKey;

    @Value("${openai.model}")
    private String model;

    @Bean
    public HttpHeaders openAiHeaders(@Value("${openai.secret-key}") String secretKey) {
        HttpHeaders headers = new HttpHeaders();

        headers.setBearerAuth(secretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        return headers;
    }
}

