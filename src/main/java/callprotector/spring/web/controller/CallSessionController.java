package callprotector.spring.web.controller;

import callprotector.spring.apiPayload.ApiResponse;
import callprotector.spring.service.CallLogService.CallLogService;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sessions")
public class CallSessionController {

    private final CallSessionService callSessionService;
    private final CallLogService callLogService;

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

    @Operation(summary = "상담 내역 조회 API", description = "카테고리별 상담 내역을 페이지네이션 방식으로 조회합니다.")
    @GetMapping("")
    public ApiResponse<CallSessionResponseDTO.CallSessionPagingDTO> getCallSessions(
            @Parameter(description = "현재 페이지의 기준이 되는 마지막 CallSession ID")
            @RequestParam(required = false) Long cursorId,

            @Parameter(description = "가져올 데이터 개수")
            @RequestParam(defaultValue = "5") int size,

            @Parameter(description = "정렬 순서 (desc, asc)")
            @RequestParam(defaultValue = "desc") String order,

            @Parameter(description = "폭언 카테고리 (verbalAbuse, sexualHarass, threat)")
            @RequestParam(required = false) String category
    ) {
        if (category != null) {
            return ApiResponse.onSuccess(callSessionService.getSessionsByAbuseCategory(category, cursorId, size, order));
        }
        return ApiResponse.onSuccess(callSessionService.getCallSessions("id", order, cursorId, size));
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

    @Operation(
            summary = "AI 상담 요약 생성 API",
            description = "CallSession ID를 기반으로 고객과 상담원의 통화 내용을 요약하여 CallSession의 summary 필드에 저장합니다."
    )
    @PostMapping("/{sessionId}/summary")
    public ApiResponse<String> generateSummary(@PathVariable Long sessionId) {
        String summary = callLogService.generateAiSummary(sessionId);
        return ApiResponse.onSuccess(summary);
    }

    @Operation(
        summary = "AI 상담 요약 생성 API - Gemini 2.5 flash",
        description = "CallSession ID를 기반으로 고객과 상담원의 통화 내용을 요약하여 CallSession의 summary_gemini 필드에 저장합니다."
    )
    @PostMapping("/{sessionId}/summary-gemini")
    public ApiResponse<CallSessionResponseDTO.CallSessionSummaryResponseDTO> generateSummaryGemini(@PathVariable Long sessionId) {
        CallSessionResponseDTO.CallSessionSummaryResponseDTO response = callSessionService.createCallSessionSummary(sessionId);
        return ApiResponse.onSuccess(response);
    }

}