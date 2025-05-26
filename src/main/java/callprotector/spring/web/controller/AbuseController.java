package callprotector.spring.web.controller;


import callprotector.spring.service.AbuseService.AbuseService;
import callprotector.spring.web.dto.request.AbuseRequestDTO;
import callprotector.spring.web.dto.response.AbuseResponseDTO;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // 해당 클래스 Controller로 지정
// @Controller + @ReseponseBody 합친 것으로, 컨트롤러 내 모든 메서드가 반환 시 객체를 자동으로 JSON 형식으로 변환
@RequestMapping("/abuse")
@RequiredArgsConstructor // private final AbuseService abuseService; 적었다면, Service 생성자 자동 생성 후 주입해줌
// final "자바에서 한 번만 할당할 수 있다" (한 번 값이 정해지면 다시 바꿀 수 X)
public class AbuseController {

    private final AbuseService abuseService; // -> 의존성 주입 시, 주입된 객체가 바뀌지 않도록 보장

    // 함수에 응답 형식 DTO, 파라미터에 요청 형식 DTO (클라이언트가 요청 보내니까 파라미터, 처리결과는 응답이니가 응답 DTO)
    @GetMapping("/abuse-filter")
    public ResponseEntity<AbuseResponseDTO.AbuseFilterDTO> filterAbuse(@RequestBody AbuseRequestDTO.AbuseFilterDTO request) {
        AbuseResponseDTO.AbuseFilterDTO response = abuseService.analyzeText(request.getText());
        return ResponseEntity.ok(response);
    }




}
