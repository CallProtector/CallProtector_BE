package callprotector.spring.global.apiPayload.exception.handler;

import callprotector.spring.global.apiPayload.code.status.ErrorStatus;
import callprotector.spring.global.apiPayload.exception.GeneralException;

public class InvalidCategoryFilterException extends GeneralException {
    public InvalidCategoryFilterException() {
        super(ErrorStatus.INVALID_CATEGORY_FILTER);
    }
}
