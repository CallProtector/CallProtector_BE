package callprotector.spring.apiPayload.exception.handler;

import callprotector.spring.apiPayload.code.status.ErrorStatus;
import callprotector.spring.apiPayload.exception.GeneralException;

public class UserNotFoundException extends GeneralException {
	public UserNotFoundException() {

      super(ErrorStatus.USER_NOT_FOUND);
	}
}
