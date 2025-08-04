package callprotector.spring.domain.callsttlog.dto.response;

import callprotector.spring.domain.callsttlog.entity.CallSttLog;

public record CallSttLogResponseDTO(
	String type,
	CallSttLog payload
) {
}
