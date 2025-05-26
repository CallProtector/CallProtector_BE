package callprotector.spring.apiPayload.exception.handler;

import callprotector.spring.apiPayload.code.BaseErrorCode;
import callprotector.spring.apiPayload.exception.GeneralException;

public class TempHandler extends GeneralException {

    public TempHandler(BaseErrorCode errorCode) {
        super(errorCode);
    }

}
