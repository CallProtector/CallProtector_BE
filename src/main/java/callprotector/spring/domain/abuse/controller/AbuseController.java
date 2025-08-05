package callprotector.spring.domain.abuse.controller;

import callprotector.spring.domain.abuse.dto.response.AbuseFilterResponseDTO;
import callprotector.spring.global.apiPayload.ApiResponse;
import callprotector.spring.domain.abuse.service.AbuseService;
import callprotector.spring.domain.abuse.dto.request.AbuseRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/abuse")
@RequiredArgsConstructor
public class AbuseController {

    private final AbuseService abuseService;

    @Operation(summary = "욕설 필터링 API", description = "욕설 필터링 API입니다.")
    @PostMapping("/filter")
    public ApiResponse<AbuseFilterResponseDTO> filterAbuse(
            @RequestBody AbuseRequestDTO.AbuseFilterDTO request) {
        return ApiResponse.onSuccess(abuseService.analyzeText(request.getText()));
    }

}
