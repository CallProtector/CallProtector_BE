package callprotector.spring.domain.callchat.controller;

import callprotector.spring.domain.callchat.dto.response.CallChatLogResponseDTO;
import callprotector.spring.domain.callchat.entity.CallChatSession;
import callprotector.spring.domain.callchat.service.CallChatLogService;
import callprotector.spring.domain.callchat.service.CallChatSessionService;
import callprotector.spring.global.annotation.UserId;
import callprotector.spring.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/call-chat-log")
@Tag(name = "CallChatLog", description = "상담별 챗봇 로그 관련 API")
public class CallChatLogController {

    private final CallChatSessionService callChatSessionService;
    private final CallChatLogService callChatLogService;


    @Operation( summary = "상담별 채팅 세션별 로그 조회 API", description = "CallChatSession에 해당하는 CallChatLog들을 조회합니다.")
    @GetMapping("/session/{sessionId}")
    public ApiResponse<CallChatLogResponseDTO.CallChatLogListResponse> getLogs(
            @UserId Long userId,
            @Parameter(description = "조회할 CallChatSession ID")
            @PathVariable Long sessionId
    ) {
        CallChatSession session = callChatSessionService.getSessionById(sessionId);
        if (!session.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("해당 세션에 접근할 권한이 없습니다.");
        }

        var logs = callChatLogService.getLogDtosBySession(sessionId);

        return ApiResponse.onSuccess(
                CallChatLogResponseDTO.CallChatLogListResponse.builder()
                        .callSessionId(session.getCallSession().getId()) // 상위에 한 번만
                        .logs(logs)
                        .build()
        );
    }


}
