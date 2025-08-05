package callprotector.spring.global.client;

import callprotector.spring.domain.abuse.dto.response.AbuseFilterResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FastClient {

    private final RestTemplate restTemplate;

    @Value("${fastapi.url}")
    private String fastApiUrl;

    public AbuseFilterResponseDTO sendTextToFastAPI(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = Map.of("text", text);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<AbuseFilterResponseDTO> response = restTemplate.postForEntity(
                    fastApiUrl,
                    entity,
                    AbuseFilterResponseDTO.class
            );

            AbuseFilterResponseDTO result = response.getBody();
            if (result == null) {
                log.warn("⚠️ FastAPI 응답이 null입니다.");
                return new AbuseFilterResponseDTO(false, false, "분석 실패(null)");
            }

            log.info("🚨 욕설 분석 결과: abuse={}, detected={}, type={}",
                    result.isAbuse(), result.isDetected(), result.getType());

            return result;

        } catch (Exception e) {
            log.error("🔥 FastAPI 호출 실패: {}", e.getMessage(), e);
            return new AbuseFilterResponseDTO(false, false, "분석 실패");
        }
    }
}
