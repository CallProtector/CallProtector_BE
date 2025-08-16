package callprotector.spring.global.apiPayload.code.status;

import callprotector.spring.global.apiPayload.code.BaseErrorCode;
import callprotector.spring.global.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {

    // 일반적인 응답
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON5000", "서버 에러, 관리자에게 문의 바랍니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST,"COMMON4000","잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"COMMON4010","인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON4030", "금지된 요청입니다."),

    // Mail 관련 예외
    MAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "MAIL4091", "이미 가입된 이메일입니다."),

    // User 관련 에러
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER4041", "사용자가 없습니다."),
    USER_VALIDATION_ERROR(HttpStatus.UNAUTHORIZED, "USER4011", "등록되지 않은 이메일입니다."),
    USER_PASSWORD_MISMATCH(HttpStatus.UNAUTHORIZED, "USER4012", "비밀번호가 일치하지 않습니다."),

    // Call Session 관련 에러
    CALL_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "CALLSESSION4041", "call session이 존재하지 않습니다."),
    CALL_SESSION_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "CALLSESSION4042", "해당 유저의 call session이 존재하지 않습니다."),
    CALL_SESSION_ALREADY_EXISTS(HttpStatus.CONFLICT, "CALLSESSION4091", "이미 존재하는 callsid 입니다."),

    // Call Stt Log 관련 에러
    CALL_STT_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "CALLSTTLOG4041", "조건에 맞는 STT LOG가 존재하지 않습니다."),
    CANT_SUMMARY_CALL_STT_LOG(HttpStatus.BAD_REQUEST, "CALLSTTLOG4001", "상담 내역 기록이 존재하지 않아 요약할 수 없습니다."),
    CALL_STT_LOG_NO_MEANINGFUL_CONTENT(HttpStatus.BAD_REQUEST, "CALLSTTLOG4002", "의미 있는 대화 내용이 없어 상담 내역을 요약할 수 없습니다."),

    // 폭언 카테고리 관련 에러
    INVALID_CATEGORY_FILTER(HttpStatus.BAD_REQUEST, "CATEGORY4001", "유효하지 않은 카테고리입니다."),

    // AI 상담 요약 관련 에러
    SUMMARY_AI_GEMINI_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AISUMMARY5001", "요약 생성 중 기술적 오류가 발생하여 상담 내역을 요약할 수 없습니다."),
    SUMMARY_AI_OPENAI_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AISUMMARY5002", "요약 생성 중 기술적 오류가 발생하여 상담 내역을 요약할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .build();
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build()
                ;
    }
}