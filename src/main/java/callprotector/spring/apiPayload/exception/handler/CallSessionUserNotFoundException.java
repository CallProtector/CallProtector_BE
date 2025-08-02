package callprotector.spring.apiPayload.exception.handler;

import callprotector.spring.apiPayload.code.status.ErrorStatus;
import callprotector.spring.apiPayload.exception.GeneralException;

public class CallSessionUserNotFoundException extends GeneralException {
	public CallSessionUserNotFoundException() {
		super(ErrorStatus.CALL_SESSION_USER_NOT_FOUND);
	}
}
