package callprotector.spring.client;

import callprotector.spring.web.dto.response.AbuseResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

// Client 폴더: 외부 API 통신 전담
// 역할: Spring 애플리케이션 내부에서 외부 서버(Flask 등)에 HTTP 요청을 보내는 모듈을 담당
// - 일반적으로 Flask, 외부 API 서버, DB 외부 서비스, Microservice 등과 통신할 때 사용
// - 통신 로직을 서비스에서 분리하여 **비즈니스 로직(Service)**이 더 깔끔하게 유지됨

@Component // Spring이 FlaskClient 객체를 자동으로 생성해서 관리하도록 만드는 것 (이 클래스를 Spring Bean으로 등록해라!!)
@RequiredArgsConstructor // Component랑 RequiredArgsConstructor랑 같이 쓰면, @Autowired 주입 없이도 사용 가능
// 왜 필요해? -> Spring에서 FlaskClient를 new로 만들지 않고, """Spring 내부에서 생성·주입 관리하도록""" 하기 위함

public class FastClient { // Service는 비즈니스 로직만 전담하고, Client는 외부 시스템과의 연결 전담

    private final RestTemplate restTemplate; // final 필드에 대해 자동으로 생성자 만들어 줌

    private final String FASTAPI_URL = "http://localhost:8000/filter-abuse";

    public AbuseResponseDTO.AbuseFilterDTO sendTextToFlask(String text) {
        Map<String, String> request = Map.of("text", text);

        // Spring의 RestTemplate: 외부 HTTP 서버에 요청을 보내는 도구
        // postForEntity = POST요청을 보내고, 응답을 ResponseEntity로 받겠다.
        ResponseEntity<AbuseResponseDTO.AbuseFilterDTO> response = restTemplate.postForEntity(
                FASTAPI_URL, // 요청 보낼 URL
                request, // 요청 본문 (JSON으로 변환됨)
                AbuseResponseDTO.AbuseFilterDTO.class // 응답 받을 DTO 클래스
        );
        return response.getBody();

    }

}

// Servie: 업무 처리자, Client: 외부 심부름꾼
