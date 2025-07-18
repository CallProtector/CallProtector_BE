package callprotector.spring.web.controller;

import callprotector.spring.apiPayload.ApiResponse;
import callprotector.spring.service.CallSessionService.CallSessionService;
import callprotector.spring.web.dto.request.CallSessionRequestDTO;
import callprotector.spring.web.dto.response.CallSessionResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CallSessionController {

    private final CallSessionService callSessionService;

    @Operation(summary = "CallSession 생성", description = "Twilio 수신 시 콜세션을 생성하고 발신번호를 저장합니다.")
    @PostMapping("/call-session")
    public ApiResponse<CallSessionResponseDTO.CallSessionMakeDTO> createCallSession(
            @AuthenticationPrincipal String email,
            @RequestBody CallSessionRequestDTO.CallSessionMakeDTO dto) {
        Long sessionId = callSessionService.createCallSession(email, dto);
        return ApiResponse.onSuccess(CallSessionResponseDTO.CallSessionMakeDTO.builder()
                .sessionId(sessionId)
                .build());


    }

    @Operation(summary = "callSession 상세 조회", description = "상담 내역 상세 조회 시 callSession을 조회합니다.")
    @GetMapping("/sessions/{callSessionId}")
    public ApiResponse<CallSessionResponseDTO.CallSessionDetailResponseDTO> getCallSession(
        @PathVariable("callSessionId") Long id
        // TODO: 유저 인증(token) 추가
    ) {
        CallSessionResponseDTO.CallSessionDetailResponseDTO response = callSessionService.getCallSessionDetail(id);
        return ApiResponse.onSuccess(response);
    }

}
