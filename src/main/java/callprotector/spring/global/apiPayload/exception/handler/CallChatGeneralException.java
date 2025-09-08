package callprotector.spring.global.apiPayload.exception.handler;

import callprotector.spring.global.apiPayload.code.status.ErrorStatus;
import callprotector.spring.global.apiPayload.exception.GeneralException;

public class CallChatGeneralException extends GeneralException {
    public CallChatGeneralException(ErrorStatus errorStatus) {
        super(errorStatus);
    }
}
