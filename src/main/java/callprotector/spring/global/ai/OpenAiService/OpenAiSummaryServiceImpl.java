package callprotector.spring.global.ai.OpenAiService;

import callprotector.spring.global.config.OpenAiConfig;
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
    public String summarize(String conversationScript) {
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", "당신은 고객 상담 내용을 요약하는 AI입니다."),
                Map.of("role", "user", "content", buildPrompt(conversationScript))
        );

        Map<String, Object> requestBody = Map.of(
                "model", openAiConfig.getModel(),
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

    private String buildPrompt(String conversation) {
        return String.format("""
        당신은 고객 상담 내용을 전문적으로 요약하는 AI 비서입니다.

        아래는 고객(INBOUND)과 상담원(OUTBOUND)의 실제 대화 내용입니다.

        이 대화에서 다음 사항을 중심으로 정확하고 구체적으로 요약해주세요:
        - 고객이 문의한 핵심 질문 또는 요청 사항
        - 상담원이 제공한 안내, 처리 절차 또는 조치 내용

        응답은 **명사형 종결 어미**로 작성하되, **핵심 내용을 빠짐없이 구체적으로** 기술해주세요.
        단순 요약이 아닌 **실제 대화 흐름에 기반한 명확한 정보 전달**을 목표로 합니다.

        다음 형식에 맞춰 작성해주세요:

        문의사항
        <고객의 요청·질문 내용을 요약한 문장>

        처리 결과
        <상담원이 제공한 정보 또는 조치 내용을 요약한 문장>

        ---

        대화 내용:
        %s
        """, conversation);
    }

}