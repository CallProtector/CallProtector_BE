package callprotector.spring.domain.callchat.controller;

import callprotector.spring.domain.callsttlog.entity.CallSttLog;
import callprotector.spring.domain.callsttlog.service.CallSttLogService;
import callprotector.spring.domain.callchat.entity.CallChatSession;
import callprotector.spring.domain.callchat.service.CallChatLogService;
import callprotector.spring.domain.callchat.service.CallChatSessionService;
import callprotector.spring.global.apiPayload.code.status.ErrorStatus;
import callprotector.spring.global.apiPayload.exception.handler.CallChatGeneralException;
import callprotector.spring.global.client.ChatbotClient;
import callprotector.spring.global.security.TokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/call-chat")
@Tag(name = "CallChatStream", description = "상담별 채팅 질문 전송 관련 API")
public class CallChatStreamController {

    private final ChatbotClient chatbotClient;

    private final CallChatLogService callChatLogService;
    private final CallChatSessionService callChatSessionService;
    private final TokenProvider tokenProvider;

    // 08/13 추가: STT 로그 조회용
    private final CallSttLogService callSttLogService;

    @Operation(
            summary = "상담별 채팅 질문 전송 API",
            description ="상담원이 입력한 법률 질문을, 문맥을 유지하고 있는 챗봇에게 전송하고 응답을 받아옵니다."
    )
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamCallChat(
            @Parameter(description = "대화가 기록될 CallChatSession ID", required = true)
            @RequestParam Long callChatSessionId,

            @Parameter(description = "질문 내용", required = true)
            @RequestParam String question,

            @Parameter(description = "JWT 토큰", required = true)
            @RequestParam String token
    ) {
        // 1) JWT → userId
        Long userId = tokenProvider.validateAndGetUserId(token);

        // 2) 세션 소유권 검증
        CallChatSession session = callChatSessionService.getSessionById(callChatSessionId);
        if (!session.getUser().getId().equals(userId)) {
            throw new CallChatGeneralException(ErrorStatus.CALLCHAT_SESSION_FORBIDDEN);
        }

        // ★★★  08/13 추가: Scripts 구성 (callchatbot 서비스 로직 재사용)
        List<Map<String, String>> contextScripts = List.of(); // 기본 빈 리스트
        if (session.getCallSession() != null) {
            Long callSessionId = session.getCallSession().getId();
            List<CallSttLog> logs = callSttLogService.getAllBySessionId(callSessionId);

            // (A) 전부 전송 (2번과 동일)
            List<Map<String, String>> scripts = new ArrayList<>(logs.size());
            for (CallSttLog log : logs) {
                scripts.add(Map.of(
                        "speaker", log.getTrack().name(),  // INBOUND / OUTBOUND
                        "text", log.getScript()
                ));
            }

            // (옵션) 페이로드 최적화: abuse 구간 ±2턴 + 총 6000자 컷
            // scripts = trimByAbuseWindowAndLength(logs, 2, 6000);

            contextScripts = scripts;
        }

        StringBuilder jsonBuffer = new StringBuilder();

        // 3) FastAPI 호출
        return chatbotClient.sendChatRequest(
                        "/ai/callchat/stream",
                        Map.of(
                                "session_id", callChatSessionId,   // 백엔드 메모리 키로 쓰고 싶으면 이 값 활용
                                "question", question,
                                "context_scripts", contextScripts
                        )
                )
                .map(raw -> {                                            // data: 조건부 제거
                    String s = raw == null ? "" : raw.trim();
                    if (s.startsWith("data:")) s = s.substring(5).trim();
                    return s;
                })
                .filter(s -> !s.isEmpty())
                .doOnNext(chunk -> {
                    if ("[END]".equals(chunk)) return;                  // 그대로 프론트로
                    if (chunk.startsWith("[ERROR]")) {                  // 에러 로그
                        log.error("SSE ERROR from FastAPI: {}", chunk);
                        return;
                    }
                    if (chunk.startsWith("[JSON]")) {                   // 버퍼 초기화 후 저장
                        jsonBuffer.setLength(0);
                        jsonBuffer.append(chunk.substring("[JSON]".length()).trim());
                    }
                })
                .doOnError(e -> log.error("SSE proxy error", e))        // 에러 핸들링
                .doOnComplete(() -> {
                    try {
                        if (jsonBuffer.length() == 0) return;           // JSON 없으면 스킵
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode node = mapper.readTree(jsonBuffer.toString());

                        String answer = node.has("answer") ? node.get("answer").asText() : "";
                        String sourcePages = (node.has("sourcePages") && !node.get("sourcePages").isNull())
                                ? node.get("sourcePages").toString()
                                : "[]";

                        callChatLogService.saveCallChatLog(
                                callChatSessionId,
                                question,
                                answer,
                                sourcePages
                        );
                    } catch (Exception e) {
                        log.error("❌ 상담별 채팅 저장 실패", e);
                        throw new CallChatGeneralException(ErrorStatus.CALLCHAT_LOG_SAVE_FAILED);
                    }
                })
                .delayElements(Duration.ofMillis(5));
    }
}
