package callprotector.spring.apiPayload.exception.handler;

import callprotector.spring.apiPayload.code.status.ErrorStatus;
import callprotector.spring.apiPayload.exception.GeneralException;

public class CallSessionExistsException extends GeneralException {
	public CallSessionExistsException() {
		super(ErrorStatus.CALL_SESSION_ALREADY_EXISTS);
	}
}
