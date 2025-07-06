package callprotector.spring.web.dto.response;

import callprotector.spring.domain.CallSttLog;

public record CallSttLogResponseDTO(
	String type,
	CallSttLog payload
) {
}
