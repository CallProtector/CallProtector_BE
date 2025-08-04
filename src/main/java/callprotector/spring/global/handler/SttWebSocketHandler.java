package callprotector.spring.global.handler;

import callprotector.spring.domain.callsession.dto.response.CallSessionResponseDTO;
import callprotector.spring.domain.callsttlog.dto.response.CallSttLogResponseDTO;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
@Slf4j
@RequiredArgsConstructor
public class SttWebSocketHandler extends TextWebSocketHandler implements ClientNotifier {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // WebSocket 세션 관리
    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionIdToUserId = new ConcurrentHashMap<>();

    // 웹 소켓 연결시
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("✅ WebSocket 연결 시도됨: sessionId={}, uri={}", session.getId(), session.getUri());

        Long userId = getUserIdFromSession(session);
        log.info("👉 추출된 userId: {}", userId);
        log.info("✅ Twilio WebSocket 연결됨: {}", session.getId());


        if (sessions.containsKey(userId)) {
            log.warn("이미 연결된 세션이 존재합니다. 새로운 연결을 거부합니다. userId: " + userId);
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        log.info("웹소켓 연결 성공입니다. 현재 연결된 userId ======== {}", userId);
        sessions.put(userId, session);
        sessionIdToUserId.put(session.getId(), userId);
        log.info("현재 세션에 접속중인 유저 목록 ======== {}", sessions.keySet());
    }

    // 데이터 통신시 (Client -> Server)
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
    }

    // 웹소켓 통신 에러시
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("웹소켓 전송 에러 발생 - sessionId: {}, error: {}", session.getId(), exception.getMessage());
        super.handleTransportError(session, exception);
    }

    // 웹 소켓 연결 종료시
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = sessionIdToUserId.remove(session.getId()); // ✅ 세션 ID로 찾음
        if (userId != null) {
            sessions.remove(userId);
            log.info("WebSocket 연결 종료됨: userId ======== {}", userId);
        } else {
            log.warn("연결 종료 시 userId를 찾을 수 없음. sessionId ======== {}", session.getId());
        }
    }

    // STT 로그 데이터 전송 - Object
    @Override
    public void sendSttToClient(Long userId, Object payload) {
        sendWebSocketMessage(userId, payload, json ->
            log.info("STT 로그 전송 완료 → userId={}, payload={}", userId, json));
    }

    // STT 로그 데이터 전송 - DTO
    @Override
    public void sendSttToClient(Long userId, CallSttLogResponseDTO sttLogResponseDTO) {
        sendWebSocketMessage(userId, sttLogResponseDTO, json ->
            log.debug("STT 로그 전송 완료 → userId={}, payload={}", userId, json));
    }

    // CallSession 정보 데이터 전송
    @Override
    public void sendSessionInfoToClient(Long userId, CallSessionResponseDTO.CallSessionInfoDTO sessionInfo) {
        ObjectNode jsonPayload = objectMapper.createObjectNode();
        jsonPayload.put("type", "sessionInfo");
        jsonPayload.put("sessionCode", sessionInfo.getCallSessionCode());
        jsonPayload.put("createdAt", sessionInfo.getCreatedAt());
        jsonPayload.put("totalAbuseCnt", sessionInfo.getTotalAbuseCnt());

        sendWebSocketMessage(userId, jsonPayload, json -> {
            log.info("세션 초기 정보 WebSocket 전송 대상 userId ======== {}", userId);
            log.info("전송 내용: {}", json);
        });
    }

    // CallSession - totalAbuseCnt 업데이트 정보 데이터 전송
    @Override
    public void sendUpdateAbuseCntToClient(Long userId, CallSessionResponseDTO.CallSessionTotalAbuseCntDTO sessionTotalAbuseCnt) {
        ObjectNode jsonPayload = objectMapper.createObjectNode();
        jsonPayload.put("type", "totalAbuseCntUpdate");
        jsonPayload.put("callSessionId", sessionTotalAbuseCnt.getSessionId());
        jsonPayload.put("totalAbuseCnt", sessionTotalAbuseCnt.getTotalAbuseCnt());

        sendWebSocketMessage(userId, jsonPayload, json -> {
            log.info("세션 totalAbuseCnt 업데이트 WebSocket 전송 대상 userId={}", userId);
            log.info("업데이트된 전송 내용: {}", json);
        });
    }

    // 특정 사용자 ID와 WebSocket 세션을 매핑하여 등록
    @Override
    public void registerUserSession(Long userId, WebSocketSession session) {
        // 이미 해당 userId로 세션이 등록되어 있는지 확인 (다중 연결 방지)
        if (sessions.containsKey(userId)) {
            WebSocketSession existingSession = sessions.get(userId);
            if (existingSession != null && existingSession.isOpen()) {
                log.warn("이미 활성 세션이 존재합니다. 새 연결로 교체합니다. userId: {}, 기존 sessionId: {}, 새 sessionId: {}",
                    userId, existingSession.getId(), session.getId());
                try {
                    existingSession.close(CloseStatus.SERVER_ERROR.withReason("새로운 STT 세션이 시작되었습니다."));
                } catch (IOException e) {
                    log.error("기존 세션 닫기 실패: {}", e.getMessage());
                }
                sessionIdToUserId.remove(existingSession.getId()); // 기존 매핑 제거
            }
        }

        sessions.put(userId, session);
        sessionIdToUserId.put(session.getId(), userId); // 세션 ID -> userId 매핑 업데이트
        log.info("사용자 {}가 STT WebSocket 세션 {}에 등록되었습니다.", userId, session.getId());
        log.info("현재 세션에 접속 중인 유저 목록: {}", sessions.keySet());
    }

    private Long getUserIdFromSession(WebSocketSession session) {
        try {
            String query = session.getUri() != null ? session.getUri().getQuery() : null;
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2 && keyValue[0].equals("userId")) {
                        return Long.parseLong(keyValue[1]);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("userId 파싱 실패: {}", e.getMessage());
        }
        return null;
    }

    private WebSocketSession validateSession(Long userId) {
        WebSocketSession session = sessions.get(userId);
        if (session == null) {
            // 세션 자체가 없는 경우
            throw new IllegalArgumentException("사용자 ID " + userId + "에 대한 WebSocket 세션을 찾을 수 없습니다.");
        }
        if (!session.isOpen()) {
            // 세션은 존재하지만 현재 닫혀있는 경우
            throw new IllegalStateException("사용자 ID " + userId + "의 WebSocket 세션이 닫혀 있습니다.");
        }
        return session;
    }

    private void sendWebSocketMessage(Long userId, Object payload, Consumer<String> successLogger) {
        try {
            WebSocketSession session = validateSession(userId);

            String json;
            if (payload instanceof ObjectNode) {
                // ObjectNode인 경우
                json = ((ObjectNode) payload).toString();
            } else if (payload instanceof Map) {
                // Map인 경우 ObjectMapper로 직렬화
                json = objectMapper.writeValueAsString(payload);
            } else {
                // 그 외 DTO 등 일반 객체
                json = objectMapper.writeValueAsString(payload);
            }

            session.sendMessage(new TextMessage(json));
            successLogger.accept(json); // 전송 성공 시 커스텀 로그 실행

        } catch (IllegalArgumentException | IllegalStateException e) { // validateSession()이 던지는 예외
            log.warn("WebSocket 전송 실패 (세션 유효성 문제): userId={}, error={}", userId, e.getMessage());
        } catch (IOException e) { // session.sendMessage()에서 발생하는 I/O 예외
            log.error("WebSocket 전송 실패 (I/O 오류): userId={}, error={}", userId, e.getMessage(), e);
        } catch (Exception e) { // objectMapper.writeValueAsString() 등 기타 예상치 못한 예외
            log.error("WebSocket 메시지 처리 중 예상치 못한 오류 발생: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
}
