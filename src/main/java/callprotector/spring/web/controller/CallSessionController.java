package callprotector.spring.web.controller;

import callprotector.spring.apiPayload.ApiResponse;
import callprotector.spring.service.CallSessionService.CallSessionService;
import callprotector.spring.web.dto.request.CallSessionRequestDTO;
import callprotector.spring.web.dto.response.CallSessionResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sessions")
public class CallSessionController {

    private final CallSessionService callSessionService;

    @Operation(summary = "CallSession 생성", description = "Twilio 수신 시 콜세션을 생성하고 발신번호를 저장합니다.")
    @PostMapping("")
    public ApiResponse<CallSessionResponseDTO.CallSessionMakeDTO> createCallSession(
            @AuthenticationPrincipal String email,
            @RequestBody CallSessionRequestDTO.CallSessionMakeDTO dto) {
        Long sessionId = callSessionService.createCallSession(email, dto);
        return ApiResponse.onSuccess(CallSessionResponseDTO.CallSessionMakeDTO.builder()
                .sessionId(sessionId)
                .build());
    }

    @Operation(summary = "상담 내역 조회 API", description = "정렬 조건을 선택하여 전체 상담 내역을 조회합니다.")
    @GetMapping("")
    public ApiResponse<List<CallSessionResponseDTO.CallSessionListDTO>> getCallSessions(
            @Parameter(description = "정렬 기준 필드 (예: createdAt)")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "정렬 순서: 최신순(desc), 오래된순(asc)")
            @RequestParam(defaultValue = "desc") String order
    ) {
        List<CallSessionResponseDTO.CallSessionListDTO> sessions = callSessionService.getCallSessions(sortBy, order);
        return ApiResponse.onSuccess(sessions);
    }

    @Operation(summary = "callSession 상세 조회", description = "상담 내역 상세 조회 시 callSession을 조회합니다.")
    @GetMapping("/{callSessionId}")
    public ApiResponse<CallSessionResponseDTO.CallSessionDetailResponseDTO> getCallSession(
            @PathVariable("callSessionId") Long id
            // TODO: 유저 인증(token) 추가
    ) {
        CallSessionResponseDTO.CallSessionDetailResponseDTO response = callSessionService.getCallSessionDetail(id);
        return ApiResponse.onSuccess(response);
    }

}