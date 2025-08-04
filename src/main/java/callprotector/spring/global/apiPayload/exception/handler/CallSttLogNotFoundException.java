package callprotector.spring.global.apiPayload.exception.handler;

import callprotector.spring.global.apiPayload.code.status.ErrorStatus;
import callprotector.spring.global.apiPayload.exception.GeneralException;

public class CallSttLogNotFoundException extends GeneralException {
	public CallSttLogNotFoundException() {
		super(ErrorStatus.CALL_STT_LOG_NOT_FOUND);
	}
}
