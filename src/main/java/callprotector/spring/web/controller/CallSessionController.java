package callprotector.spring.web.controller;

import callprotector.spring.apiPayload.ApiResponse;
import callprotector.spring.service.CallSessionService.CallSessionService;
import callprotector.spring.web.dto.request.CallSessionRequestDTO;
import callprotector.spring.web.dto.response.CallSessionResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/call-session")
public class CallSessionController {

    private final CallSessionService callSessionService;

    @Operation(summary = "콜세션 API", description = "만들어 놓긴 했는데, 사용 안 하는 API입니다.")
    @PostMapping
    public ApiResponse<CallSessionResponseDTO.CallSessionMakeDTO> createCallSession(@AuthenticationPrincipal String email, @RequestBody CallSessionRequestDTO.CallSessionMakeDTO dto){
        Long sessionId = callSessionService.createCallSession(email, dto);
        return ApiResponse.onSuccess(CallSessionResponseDTO.CallSessionMakeDTO.builder()
                .sessionId(sessionId)
                .build());
    }

}
