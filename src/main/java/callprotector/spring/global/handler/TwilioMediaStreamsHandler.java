package callprotector.spring.global.handler;

import callprotector.spring.global.client.FastClient;
import callprotector.spring.domain.calllog.service.CallLogService;
import callprotector.spring.domain.callsession.service.CallSessionService;
import callprotector.spring.domain.callsttlog.service.CallSttLogService;

import com.fasterxml.jackson.databind.ObjectMapper;

import callprotector.spring.domain.user.service.UserService;
import callprotector.spring.global.multimodal.ShoutingDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Component
@Slf4j
public class TwilioMediaStreamsHandler extends AbstractWebSocketHandler {

    private final ObjectMapper mapper;
    private final FastClient fastClient;
    private final CallSessionService callSessionService;
    private final CallLogService callLogService;
    private final CallSttLogService callSttLogService;
    private final UserService userService;
    private final ClientNotifier sttWebSocketHandler;
    private final TwilioSessionManager sessionManager;

    private final Map<String, TwilioMediaStreamProcessor> activeProcessors = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("✅ WebSocket 연결됨: {}", session.getId());

        ShoutingDetector shoutingDetector = new ShoutingDetector();

        TwilioMediaStreamProcessor processor = new TwilioMediaStreamProcessor(
            this.mapper,
            this.fastClient,
            this.callSessionService,
            this.callLogService,
            this.callSttLogService,
            this.userService,
            this.sttWebSocketHandler,
            this.sessionManager,
            shoutingDetector
        );
        activeProcessors.put(session.getId(), processor);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        TwilioMediaStreamProcessor processor = activeProcessors.get(session.getId());

        if (processor == null) {
            log.warn("❗ 세션 {}에 대한 프로세서를 찾을 수 없습니다. 메시지 처리 불가.", session.getId());
            session.close(CloseStatus.SERVER_ERROR.withReason("프로세서가 초기화되지 않았습니다."));
            return;
        }

        processor.handleTwilioMessage(session, message);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("✅ WebSocket 연결 종료됨: {} (상태: {})", session.getId(), status.getCode());

        TwilioMediaStreamProcessor processor = activeProcessors.remove(session.getId());

        if (processor == null) {
            log.warn("❗ 연결 종료 시 세션 {}에 대한 프로세서를 찾을 수 없음.", session.getId());
            return;
        }

        String callSid = processor.getPrimaryCallSid();
        if (callSid != null) {
            sessionManager.unregisterProcessor(callSid);
        } else {
            log.warn("❗ 종료된 세션 {}에 CallSid가 없어 TwilioSessionManager에서 제거할 수 없습니다.", session.getId());
        }

        processor.closeSession();
        log.info("✅ 세션 {}의 TwilioMediaStreamProcessor 정리 완료.", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("❌ WebSocket 전송 오류 : sessionId={}", session.getId(), exception);
        TwilioMediaStreamProcessor processor = activeProcessors.get(session.getId());

        if (processor == null) {
            log.warn("❗ 전송 오류 발생 시 세션 {}에 대한 프로세서를 찾을 수 없음.", session.getId());
            return;
        }

        processor.handleError(exception);
    }
}
