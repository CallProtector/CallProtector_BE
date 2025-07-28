package callprotector.spring.apiPayload.exception.handler;

import callprotector.spring.apiPayload.code.status.ErrorStatus;
import callprotector.spring.apiPayload.exception.GeneralException;

public class CallSessionSummaryGenerationException extends GeneralException {
	public CallSessionSummaryGenerationException(ErrorStatus errorStatus) {
		super(errorStatus);
	}
}
