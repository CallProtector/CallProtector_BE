package callprotector.spring.apiPayload.exception.handler;

import callprotector.spring.apiPayload.code.status.ErrorStatus;
import callprotector.spring.apiPayload.exception.GeneralException;

public class InvalidCategoryFilterException extends GeneralException {
    public InvalidCategoryFilterException() {
        super(ErrorStatus.INVALID_CATEGORY_FILTER);
    }
}
