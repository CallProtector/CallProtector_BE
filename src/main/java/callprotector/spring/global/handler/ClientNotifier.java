package callprotector.spring.global.handler;

import callprotector.spring.domain.callsession.dto.response.CallSessionResponseDTO;
import callprotector.spring.domain.callsttlog.dto.response.CallSttLogResponseDTO;
import org.springframework.web.socket.WebSocketSession;

// 클라이언트에게 STT 결과 및 세션 정보를 WebSocket을 통해 전송하는 기능을 추상화하는 인터페이스
public interface ClientNotifier {
	void sendSttToClient(Long userId, CallSttLogResponseDTO sttLogResponseDTO);
	void sendSttToClient(Long userId, Object payload);
	// void sendSessionInfoToClient(Long userId, CallSessionResponseDTO.CallSessionInfoDTO sessionInfo);
	void sendUpdateAbuseCntToClient(Long userId, CallSessionResponseDTO.CallSessionTotalAbuseCntDTO sessionTotalAbuseCnt);
	// void registerUserSession(Long userId, WebSocketSession session);
}
