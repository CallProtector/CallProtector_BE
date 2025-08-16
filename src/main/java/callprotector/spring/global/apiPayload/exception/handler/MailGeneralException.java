package callprotector.spring.global.apiPayload.exception.handler;

import callprotector.spring.global.apiPayload.code.status.ErrorStatus;
import callprotector.spring.global.apiPayload.exception.GeneralException;

public class MailGeneralException extends GeneralException {
    public MailGeneralException(ErrorStatus errorStatus) {
        super(errorStatus);
    }
}
