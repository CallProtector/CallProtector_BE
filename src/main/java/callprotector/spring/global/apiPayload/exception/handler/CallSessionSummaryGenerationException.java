package callprotector.spring.global.apiPayload.exception.handler;

import callprotector.spring.global.apiPayload.code.status.ErrorStatus;
import callprotector.spring.global.apiPayload.exception.GeneralException;

public class CallSessionSummaryGenerationException extends GeneralException {
	public CallSessionSummaryGenerationException(ErrorStatus errorStatus) {
		super(errorStatus);
	}
}
