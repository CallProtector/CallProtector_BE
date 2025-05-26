package callprotector.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
// config 폴더: 설정 파일 전용
// - 역할: Spring의 전역 설정을 담당
// - - 공통적으로 애플리케이션 전체에서 사용하는 구성 요소(Bean)를 정의
// - - ex) RestTemplate, WebClient, CORS 설정, Swagger 설정, ObjectMapper 설정 등


@Configuration
// Spring의 설정파일 (application.yml과 달리, 자바 코드로 Bean 등록하는 방식)
// 역할: 이 클래스 안에 있는 메서드들을 보고, Spring이 Bean 객체 생성하고 관리함
// 이 클래스는 설정용 클래스다 ! Spring이 특별하게 취급해야 해 !
public class RestTemplateConfig {

    @Bean // 해당 메서드의 리턴 값을 Spring이 Bean으로 등록하라 ㅇㅇ
    // 즉 -> RestTemplate 객체를 Spring 컨테이너가 관리하도록 만드는 것.
    // 역할: 나중에 다른 클래스에서 @Autowired나 @RequiredArgsConstructor를 쓰면, 이 Bean이 자동으로 주입됨.
    public RestTemplate restTemplate() {
        return new RestTemplate();
    } // 이 메서드가 리턴하는 RestTemplate 인스턴스를 하나 만들어서 Spring이 관리해준다 ㅇㅇ
}


// 결과적으로 무슨 일이 벌어지냐면...
// 1. RestTemplateConfig 클래스 → 설정 클래스 (@Configuration)
// 2. restTemplate() 메서드 실행 → Bean 등록 (@Bean)
// 3. """다른 클래스에서 의존성 주입"""으로 사용 가능!