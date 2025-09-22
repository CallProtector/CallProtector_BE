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
    VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "VERIFY4041", "인증 요청이 존재하지 않습니다."),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "VERIFY4001", "인증 코드가 만료되었습니다."),
    VERIFICATION_CODE_INVALID(HttpStatus.BAD_REQUEST, "VERIFY4002", "인증 코드가 올바르지 않습니다."),

    // Auth 관련 예외
    EMAIL_NOT_VERIFIED(HttpStatus.UNAUTHORIZED, "VERIFY4011", "이메일 인증이 완료되지 않았습니다."),
    PASSWORD_POLICY_VIOLATION(HttpStatus.BAD_REQUEST, "USER4001", "비밀번호는 8~16자이며, 영문, 숫자, 특수문자를 모두 포함해야 합니다."),

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

    // Chat 관련 에러
    CHAT_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT4041", "세션을 찾을 수 없습니다."),
    CHAT_SESSION_FORBIDDEN(HttpStatus.FORBIDDEN, "CHAT4031", "해당 세션에 접근할 권한이 없습니다."),
    CHAT_LOG_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CHAT5001", "채팅 로그 저장에 실패했습니다."),
    CHAT_LOG_PARSE_ERROR(HttpStatus.BAD_REQUEST, "CHAT4001", "채팅 로그 파싱에 실패했습니다."),

    // CallChat 관련 에러
    CALLCHAT_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "CALLCHAT4041", "상담 기반 세션을 찾을 수 없습니다."),
    CALLCHAT_SESSION_FORBIDDEN(HttpStatus.FORBIDDEN, "CALLCHAT4031", "해당 상담 기반 세션에 접근할 권한이 없습니다."),
    CALLCHAT_LOG_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CALLCHAT5001", "상담별 채팅 로그 저장에 실패했습니다."),
    CALLCHAT_LOG_PARSE_ERROR(HttpStatus.BAD_REQUEST, "CALLCHAT4001", "상담별 채팅 로그 파싱에 실패했습니다."),
    CALLCHAT_LOG_DOES_NOT_EXISTS(HttpStatus.NOT_FOUND, "CALLCHAT4042", "상담별 채팅 로그가 존재하지 않습니다."),

    // AI 상담 요약 관련 에러
    SUMMARY_AI_GEMINI_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AISUMMARY5001", "요약 생성 중 기술적 오류가 발생하여 상담 내역을 요약할 수 없습니다."),
    SUMMARY_AI_OPENAI_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AISUMMARY5002", "요약 생성 중 기술적 오류가 발생하여 상담 내역을 요약할 수 없습니다."),

    // SMS 발송 관련 에러
    SMS_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SMS5001", "강제 통화 종료 사유 SMS 발송에 실패했습니다.");

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