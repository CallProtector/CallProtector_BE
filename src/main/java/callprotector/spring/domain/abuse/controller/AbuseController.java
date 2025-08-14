package callprotector.spring.domain.abuse.controller;

import callprotector.spring.domain.abuse.dto.response.AbuseResponseDTO;
import callprotector.spring.global.apiPayload.ApiResponse;
import callprotector.spring.domain.abuse.service.AbuseService;
import callprotector.spring.domain.abuse.dto.request.AbuseRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/abuse")
@Tag(name = "Abuse", description = "폭언 필터링 API")
public class AbuseController {

    private final AbuseService abuseService;

    @Operation(
            summary = "폭언 필터링 API",
            description = "입력된 텍스트에 대해 폭언(욕설, 성희롱, 협박) 여부를 판별합니다."
    )
    @PostMapping("/filter")
    public ApiResponse<AbuseResponseDTO.AbuseFilterDTO> filterAbuse(
            @RequestBody AbuseRequestDTO.AbuseFilterDTO request) {
        return ApiResponse.onSuccess(abuseService.analyzeText(request.getText()));
    }

}
