package callprotector.spring.domain.callchat.service;

import callprotector.spring.domain.callsession.service.CallSessionService;
import callprotector.spring.domain.callsttlog.entity.CallSttLog;
import callprotector.spring.domain.callsttlog.service.CallSttLogService;
import callprotector.spring.domain.user.service.UserService;
import callprotector.spring.global.apiPayload.code.status.ErrorStatus;
import callprotector.spring.global.apiPayload.exception.handler.CallChatGeneralException;
import callprotector.spring.global.client.ChatbotClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CallChatbotServiceImpl implements CallChatbotService {

    private final ChatbotClient chatbotClient;

    private final CallSttLogService callSttLogService;

    // 저장을 위한 추가 주입
    private final CallSessionService callSessionService;
    private final UserService userService;
    private final CallChatSessionService callChatSessionService;
    private final CallChatLogService callChatLogService;

    @Override
    public Flux<ServerSentEvent<String>> analyzeCallsession(Long sessionId, Long userId) {

        // 유저 & 콜 세션 조회
        var user = userService.getUserById(userId);
        var callSession = callSessionService.getCallSession(sessionId);

        // CallChatSession 조회 or 생성
        var callChatSession = callChatSessionService.getOrCreate(user, callSession);

        // MongoDB 에서 script 조회
        List<CallSttLog> logs = callSttLogService.getAllBySessionId(sessionId);
        if (logs.isEmpty()) {
            throw new CallChatGeneralException(ErrorStatus.CALLCHAT_LOG_DOES_NOT_EXISTS);
            // 필요하다면 CALLCHAT_NO_STT_LOG 같은 코드 추가 가능
        }

        // STT 스크립트 -> FastAPI 요청 페이로드 구성
        List<Map<String, String>> scripts = logs.stream()
                .map(log -> Map.of(
                        "speaker", log.getTrack().name(),  // INBOUND / OUTBOUND
                        "text", log.getScript()
                ))
                .toList();

        // [JSON] 청크 버퍼
        StringBuilder jsonBuffer = new StringBuilder();

        return chatbotClient.sendChatRequest(
                        "/ai/callsession/analyze",
                        Map.of(
                                "sessionId", sessionId,
                                "userId", userId,
                                "scripts", scripts
                        )
                )
                // 1) "data:" 접두사는 있을 수도/없을 수도 → 조건부 제거
                .map(raw -> {
                    String s = raw == null ? "" : raw.trim();
                    if (s.startsWith("data:")) s = s.substring(5).trim();
                    return s;
                })
                .filter(s -> !s.isEmpty())
                // 2) [JSON] 블록만 버퍼링
                .doOnNext(chunk -> {
                    if (chunk.startsWith("[JSON]")) {
                        jsonBuffer.setLength(0); // 혹시 이전 값 남아있지 않도록
                        jsonBuffer.append(chunk.substring("[JSON]".length()).trim());
                    }
                })
                // 3) 스트림 종료 시 저장
                .doOnComplete(() -> {
                    try {
                        if (jsonBuffer.length() == 0) return;

                        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        var node = mapper.readTree(jsonBuffer.toString());

                        String answer = node.has("answer") ? node.get("answer").asText() : "";
                        String sourcePages = node.has("sourcePages") ? node.get("sourcePages").toString() : "[]";

                        String question = "[초기 분석] " + callSession.getCallSessionCode();
                        callChatLogService.saveCallChatLog(
                                callChatSession.getId(),
                                question,
                                answer,
                                sourcePages
                        );
                    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                        log.error("❌ 초기 분석 JSON 파싱 실패", e);
                        throw new CallChatGeneralException(ErrorStatus.CALLCHAT_LOG_PARSE_ERROR);
                    } catch (Exception e) {
                        log.error("❌ 초기 분석 결과 저장 실패", e);
                        throw new CallChatGeneralException(ErrorStatus.CALLCHAT_LOG_SAVE_FAILED);
                    }
                })
                // 4) 프론트로는 원문 그대로 전달
                .map(chunk -> ServerSentEvent.builder(chunk).build());
    }

}
