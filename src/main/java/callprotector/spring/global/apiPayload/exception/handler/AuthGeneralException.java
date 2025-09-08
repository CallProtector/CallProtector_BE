package callprotector.spring.global.apiPayload.exception.handler;

import callprotector.spring.global.apiPayload.code.status.ErrorStatus;
import callprotector.spring.global.apiPayload.exception.GeneralException;

public class AuthGeneralException extends GeneralException {
    public AuthGeneralException(ErrorStatus errorStatus) {super(errorStatus);}
}
