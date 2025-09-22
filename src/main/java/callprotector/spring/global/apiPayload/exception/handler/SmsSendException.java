package callprotector.spring.global.apiPayload.exception.handler;

import callprotector.spring.global.apiPayload.code.status.ErrorStatus;
import callprotector.spring.global.apiPayload.exception.GeneralException;

public class SmsSendException extends GeneralException {
    public SmsSendException() { super(ErrorStatus.SMS_SEND_FAILED); }
}
