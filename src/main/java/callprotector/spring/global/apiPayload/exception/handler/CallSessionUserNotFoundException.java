package callprotector.spring.global.apiPayload.exception.handler;

import callprotector.spring.global.apiPayload.code.status.ErrorStatus;
import callprotector.spring.global.apiPayload.exception.GeneralException;

public class CallSessionUserNotFoundException extends GeneralException {
	public CallSessionUserNotFoundException() {
		super(ErrorStatus.CALL_SESSION_USER_NOT_FOUND);
	}
}
