package callprotector.spring.global.apiPayload.exception.handler;

import callprotector.spring.global.apiPayload.code.status.ErrorStatus;
import callprotector.spring.global.apiPayload.exception.GeneralException;

public class UserValidationException extends GeneralException {
	public UserValidationException() {
		super(ErrorStatus.USER_VALIDATION_ERROR);
	}
}
