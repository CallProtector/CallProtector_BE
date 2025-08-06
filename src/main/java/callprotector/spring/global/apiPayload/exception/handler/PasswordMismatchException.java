package callprotector.spring.global.apiPayload.exception.handler;

import callprotector.spring.global.apiPayload.code.status.ErrorStatus;
import callprotector.spring.global.apiPayload.exception.GeneralException;

public class PasswordMismatchException extends GeneralException {
	public PasswordMismatchException() {
		super(ErrorStatus.USER_PASSWORD_MISMATCH);
	}
}
