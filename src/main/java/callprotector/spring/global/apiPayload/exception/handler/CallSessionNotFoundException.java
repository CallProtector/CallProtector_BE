package callprotector.spring.global.apiPayload.exception.handler;

import callprotector.spring.global.apiPayload.code.status.ErrorStatus;
import callprotector.spring.global.apiPayload.exception.GeneralException;

public class CallSessionNotFoundException extends GeneralException {
	public CallSessionNotFoundException() {
		super(ErrorStatus.CALL_SESSION_NOT_FOUND);
	}
}
