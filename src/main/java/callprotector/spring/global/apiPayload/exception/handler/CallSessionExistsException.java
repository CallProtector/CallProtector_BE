package callprotector.spring.global.apiPayload.exception.handler;

import callprotector.spring.global.apiPayload.code.status.ErrorStatus;
import callprotector.spring.global.apiPayload.exception.GeneralException;

public class CallSessionExistsException extends GeneralException {
	public CallSessionExistsException() {
		super(ErrorStatus.CALL_SESSION_ALREADY_EXISTS);
	}
}
