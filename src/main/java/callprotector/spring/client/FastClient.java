package callprotector.spring.client;

import callprotector.spring.web.dto.response.AbuseResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

// Client 폴더: 외부 API 통신 전담
// 역할: Spring 애플리케이션 내부에서 외부 서버(Flask 등)에 HTTP 요청을 보내는 모듈을 담당
// - 일반적으로 Flask, 외부 API 서버, DB 외부 서비스, Microservice 등과 통신할 때 사용
// - 통신 로직을 서비스에서 분리하여 **비즈니스 로직(Service)**이 더 깔끔하게 유지됨

@Slf4j // 자동 로깅 라이브러리
@Component // Spring이 FlaskClient 객체를 자동으로 생성해서 관리하도록 만드는 것 (이 클래스를 Spring Bean으로 등록해라!!)
@RequiredArgsConstructor // Component랑 RequiredArgsConstructor랑 같이 쓰면, @Autowired 주입 없이도 사용 가능
// 왜 필요해? -> Spring에서 FlaskClient를 new로 만들지 않고, """Spring 내부에서 생성·주입 관리하도록""" 하기 위함

public class FastClient { // Service는 비즈니스 로직만 전담하고, Client는 외부 시스템과의 연결 전담

    private final RestTemplate restTemplate; // final 필드에 대해 자동으로 생성자 만들어 줌

    private static final String FASTAPI_URL = "https://callprotect.site/filter-abuse";

    public AbuseResponseDTO.AbuseFilterDTO sendTextToFastAPI(String text) {
        // 1. 요청 헤더 설정 (Content-Type: application/json)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = Map.of("text", text);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        // Spring의 RestTemplate: 외부 HTTP 서버에 요청을 보내는 도구
        // postForEntity = POST요청을 보내고, 응답을 ResponseEntity로 받겠다.
        ResponseEntity<AbuseResponseDTO.AbuseFilterDTO> response = restTemplate.postForEntity(
                FASTAPI_URL, // 요청 보낼 URL
                entity, // 요청 본문 (JSON으로 변환됨)
                AbuseResponseDTO.AbuseFilterDTO.class // 응답 받을 DTO 클래스
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

// Servie: 업무 처리자, Client: 외부 심부름꾼
