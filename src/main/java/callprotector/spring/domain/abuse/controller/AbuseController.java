package callprotector.spring.domain.abuse.controller;

import callprotector.spring.global.apiPayload.ApiResponse;
import callprotector.spring.domain.abuse.service.AbuseService;
import callprotector.spring.domain.abuse.dto.request.AbuseRequestDTO;
import callprotector.spring.domain.abuse.dto.response.AbuseResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/abuse")
@RequiredArgsConstructor
public class AbuseController {

    private final AbuseService abuseService;

    @Operation(summary = "욕설 필터링 API", description = "욕설 필터링 API입니다.")
    @PostMapping("/filter")
    public ApiResponse<AbuseResponseDTO.AbuseFilterDTO> filterAbuse(
            @RequestBody AbuseRequestDTO.AbuseFilterDTO request) {
        return ApiResponse.onSuccess(abuseService.analyzeText(request.getText()));
    }

}
