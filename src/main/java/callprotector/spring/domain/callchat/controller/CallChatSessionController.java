package callprotector.spring.domain.callchat.controller;

import callprotector.spring.domain.callchat.dto.response.CallChatSessionResponseDTO;
import callprotector.spring.domain.callchat.service.CallChatSessionService;
import callprotector.spring.global.annotation.UserId;
import callprotector.spring.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/call-chat-sessions")
@Tag(name = "CallChatSession", description = "상담별 챗봇 세션 관련 API")
public class CallChatSessionController {

    private final CallChatSessionService callChatSessionService;

    @Operation(summary = "상담별 채팅 세션 목록 조회 API", description = "유저에 해당하는 상담별 챗봇 채팅 세션을 조회합니다.")
    @GetMapping("/list")
    public ApiResponse<List<CallChatSessionResponseDTO.CallChatSessionResponse>> getSessionList(@UserId Long userId) {
        return ApiResponse.onSuccess(callChatSessionService.getSessionListDtoByUserId(userId));
    }


}
