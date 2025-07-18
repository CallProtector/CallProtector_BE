package callprotector.spring.apiPayload.exception.handler;

import callprotector.spring.apiPayload.code.status.ErrorStatus;
import callprotector.spring.apiPayload.exception.GeneralException;

public class CallSessionNotFoundException extends GeneralException {
	public CallSessionNotFoundException() {
		super(ErrorStatus.CALL_SESSION_NOT_FOUND);
	}
}
