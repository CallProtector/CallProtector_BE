package callprotector.spring.global.ai.OpenAiService;

import callprotector.spring.global.config.OpenAiConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class OpenAiTitleServiceImpl implements OpenAiTitleService {

    private final RestTemplate restTemplate;
    private final HttpHeaders openAiHeaders;
    private final OpenAiConfig openAiConfig;

    @Override
    public String generateTitle(String question) {
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", "너는 제목 생성 전문가야. 질문을 간결히 요약해 6~8자의 짧은 제목으로 만들고, 불필요한 설명은 하지 마."),
                Map.of("role", "user", "content", buildPrompt(question))
        );

        Map<String, Object> requestBody = Map.of(
                "model", openAiConfig.getModel(),
                "messages", messages,
                "temperature", 0.3
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, openAiHeaders);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.openai.com/v1/chat/completions", entity, Map.class
        );

        Map<String, Object> choice = ((List<Map<String, Object>>) response.getBody().get("choices")).get(0);
        Map<String, String> message = (Map<String, String>) choice.get("message");

        return message.get("content").trim();
    }

    private String buildPrompt(String question) {
        return String.format("""
            다음 질문을 기반으로 6~10자의 짧은 제목을 생성하세요.
            질문: %s
            
            출력은 제목만 반환하세요. 불필요한 설명 없이 제목만 출력.
        """, question);
    }
}
