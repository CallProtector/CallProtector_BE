package callprotector.spring.apiPayload.exception.handler;

import callprotector.spring.apiPayload.code.status.ErrorStatus;
import callprotector.spring.apiPayload.exception.GeneralException;

public class CallSttLogNotFoundException extends GeneralException {
	public CallSttLogNotFoundException() {
		super(ErrorStatus.CALL_STT_LOG_NOT_FOUND);
	}
}
