package callprotector.spring.global.client;

import callprotector.spring.domain.abuse.dto.response.AbuseResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private static final String FASTAPI_URL = "http://localhost:8080/filter-abuse";

    public AbuseResponseDTO.AbuseFilterDTO sendTextToFastAPI(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = Map.of("text", text);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<AbuseResponseDTO.AbuseFilterDTO> response = restTemplate.postForEntity(
                FASTAPI_URL,
                entity,
                AbuseResponseDTO.AbuseFilterDTO.class
        );

        AbuseResponseDTO.AbuseFilterDTO result = response.getBody();
        if (result == null) {
            log.warn("⚠️ FastAPI 응답이 null입니다.");
            return null;
        }
        log.info("🚨 욕설 분석 결과: abuse={}, detected={}, type={}",
                result.isAbuse(), result.isDetected(), result.getType());

        return result;

    }

}