package callprotector.spring.service.OpenAiService;

import callprotector.spring.config.OpenAiConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class OpenAiSummaryServiceImpl implements OpenAiSummaryService {

    private final RestTemplate restTemplate;
    private final HttpHeaders openAiHeaders;
    private final OpenAiConfig openAiConfig;

    @Override
    public String summarize(String inboundScript, String outboundScript) {
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", "당신은 고객 상담 내용을 요약하는 AI입니다."),
                Map.of("role", "user", "content", buildPrompt(inboundScript, outboundScript))
        );

        Map<String, Object> requestBody = Map.of(
                "model", openAiConfig.getModel(), // e.g., gpt-4o
                "messages", messages,
                "temperature", 0.4
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, openAiHeaders);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.openai.com/v1/chat/completions", entity, Map.class
        );

        Map<String, Object> choice = ((List<Map<String, Object>>) response.getBody().get("choices")).get(0);
        Map<String, String> message = (Map<String, String>) choice.get("message");

        return message.get("content");
    }

    private String buildPrompt(String inbound, String outbound) {
        return String.format("""
                다음은 고객과 상담원 간의 통화 내용입니다. 이 내용을 바탕으로 1. 문의 사항, 2. 처리 결과로 나누어 요약해 주세요.

                [고객 발화]
                %s

                [상담원 발화]
                %s

                요약 형식:
                1. 문의 사항:
                2. 처리 결과:
                """, inbound, outbound);
    }
}