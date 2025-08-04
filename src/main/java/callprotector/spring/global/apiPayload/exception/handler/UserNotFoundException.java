package callprotector.spring.global.apiPayload.exception.handler;

import callprotector.spring.global.apiPayload.code.status.ErrorStatus;
import callprotector.spring.global.apiPayload.exception.GeneralException;

public class UserNotFoundException extends GeneralException {
	public UserNotFoundException() {

      super(ErrorStatus.USER_NOT_FOUND);
	}
}
