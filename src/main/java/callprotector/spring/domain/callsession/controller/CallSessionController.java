package callprotector.spring.domain.callsession.controller;

import callprotector.spring.global.annotation.UserId;
import callprotector.spring.global.apiPayload.ApiResponse;
import callprotector.spring.domain.callsession.service.CallSessionService;
import callprotector.spring.domain.callsession.dto.request.CallSessionRequestDTO;
import callprotector.spring.domain.callsession.dto.response.CallSessionResponseDTO;
import callprotector.spring.global.handler.TwilioMediaStreamProcessor;
import callprotector.spring.global.handler.TwilioSessionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/call-sessions")
@Tag(name = "CallSession", description = "상담 내역 관련 API")
public class CallSessionController {

    private final CallSessionService callSessionService;
    private final TwilioSessionManager twilioSessionManager;

    @Operation(
            summary = "상담 내역 전체 조회 API",
            description = "검색어(keyword), 폭언 카테고리(category), 정렬 순서(order), 커서 기반 페이지네이션(cursorId)을 기반으로 상담 내역을 조회합니다."
    )
    @GetMapping("")
    public ApiResponse<CallSessionResponseDTO.CallSessionPagingDTO> getCallSessions(
        @UserId Long userId,

        @Parameter(description = "검색 키워드")
        @RequestParam(required = false) String keyword,

        @Parameter(description = "현재 페이지의 기준이 되는 마지막 CallSession ID")
        @RequestParam(required = false) Long cursorId,

        @Parameter(description = "가져올 데이터 개수 (기본값: 5)")
        @RequestParam(defaultValue = "5") int size,

        @Parameter(description = "정렬 순서 (desc: 최신순, asc: 오래된 순)")
        @RequestParam(defaultValue = "desc") String order,

        @Parameter(description = "폭언 유형 카테고리 (verbalAbuse | sexualHarass | threat)")
        @RequestParam(required = false) String category
    ) {
        // 키워드가 존재하는 경우: Elasticsearch 검색 수행
        if (keyword != null && !keyword.isBlank()) {
            log.info("🔍 키워드 검색 요청 - keyword={}, category={}, order={}, cursorId={}, size={}",
                    keyword, category, order, cursorId, size);
            return ApiResponse.onSuccess(
                    callSessionService.searchCallSessions(userId, keyword, category, order, cursorId, size)
            );
        }

        // 카테고리만 존재하는 경우: 필터 기반 조회
        if (category != null && !category.isBlank()) {
            return ApiResponse.onSuccess(
                    callSessionService.getSessionsByAbuseCategory(userId, category, cursorId, size, order)
            );
        }

        // 기본 전체 조회 (정렬 기준: ID)
        return ApiResponse.onSuccess(
                callSessionService.getCallSessions(userId, "id", order, cursorId, size)
        );
    }

    @Operation(
            summary = "상담 내역 상세 조회 API",
            description = "상담 내역 상세 조회 시 callSession을 조회합니다."
    )
    @GetMapping("/{callSessionId}")
    public ApiResponse<CallSessionResponseDTO.CallSessionDetailResponseDTO> getCallSession(
        @PathVariable("callSessionId") Long id,
        @UserId Long userId
    ) {
        CallSessionResponseDTO.CallSessionDetailResponseDTO response = callSessionService.getUserCallSessionDetail(id, userId);
        return ApiResponse.onSuccess(response);
    }

    @Operation(
            summary = "AI 상담 요약 생성 API - OpenAI GPT",
            description = "CallSession ID를 기반으로 고객과 상담원의 통화 내용을 요약하여 CallSession의 summary_simple 필드에 저장합니다."
    )
    @PostMapping("/{callSessionId}/summary/simple")
    public ApiResponse<CallSessionResponseDTO.CallSessionSummaryResponseDTO> generateSummaryOpenAi(
        @PathVariable("callSessionId") Long sessionId,
        @UserId Long userId
    ) {
        CallSessionResponseDTO.CallSessionSummaryResponseDTO response = callSessionService.createCallSessionSummaryByOpenAi(sessionId, userId);
        return ApiResponse.onSuccess(response);
    }

    @Operation(
            summary = "AI 상담 요약 생성 API - Gemini 2.5 flash",
            description = "CallSession ID를 기반으로 고객과 상담원의 통화 내용을 요약하여 CallSession의 summary_detailed 필드에 저장합니다."
    )
    @PostMapping("/{callSessionId}/summary/detailed")
    public ApiResponse<CallSessionResponseDTO.CallSessionSummaryResponseDTO> generateSummaryGemini(
        @PathVariable("callSessionId") Long sessionId,
        @UserId Long userId
    ) {
        CallSessionResponseDTO.CallSessionSummaryResponseDTO response = callSessionService.createCallSessionSummaryByGemini(sessionId, userId);
        return ApiResponse.onSuccess(response);
    }

    @Operation(
            summary = "전화 수락 시 callSession 생성 API",
            description = "전화 수락 시 해당 userId로 callsession을 생성하고 세션 정보를 반환합니다."
    )
    @PatchMapping("/user")
    public ApiResponse<CallSessionResponseDTO.CallSessionInfoDTO> acceptCallFromClient(
        @RequestBody CallSessionRequestDTO.CallSessionMakeDTO request,
        @UserId Long userId
    ) {
        log.info("📞 Client accepted call.  originalInboundCallSid: {}, callerNumber: {}, UserId: {}",
             request.getOriginalInboundCallSid(), request.getCallerNumber(), userId);
        CallSessionResponseDTO.CallSessionInfoDTO response = callSessionService.registerAcceptedUser(request, userId);

        // 실시간 세션(SttContext) 업데이트
        TwilioMediaStreamProcessor processor = twilioSessionManager.getProcessorByCallSid(request.getOriginalInboundCallSid());
        if (processor != null) {
            processor.updateUserId(userId);
        } else {
            log.warn("해당 originalInboundCallSid({})에 대한 진행 중인 통화가 없습니다.", request.getOriginalInboundCallSid());
        }

        log.info("🧾 생성된 CallSession 정보: sessionCode = {}, createdAt = {}, totalAbuseCnt = {}",
            response.getCallSessionCode(), response.getCreatedAt(), response.getTotalAbuseCnt());

        return ApiResponse.onSuccess(response);
    }

    @Operation(
            summary = "폭언 상담 내역 조회 API",
            description = "폭언(욕설, 성희롱, 협박)이 발생한 상담 내역을 조회합니다."
    )
    @GetMapping("/abusive")
    public ApiResponse<CallSessionResponseDTO.AbusiveCallSessionPagingDTO> getAbusiveCallSessions(
            @UserId Long userId,

            @Parameter(description = "현재 페이지의 기준이 되는 마지막 CallSession ID")
            @RequestParam(required = false) Long cursorId,

            @Parameter(description = "가져올 데이터 개수 (기본값: 5)")
            @RequestParam(defaultValue = "5") int size
    ) {
        return ApiResponse.onSuccess(
                callSessionService.getAbusiveCallSessions(userId, cursorId, size)
        );
    }

}
