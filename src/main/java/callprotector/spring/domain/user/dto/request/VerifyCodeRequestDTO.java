package callprotector.spring.domain.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class VerifyCodeRequestDTO {
    @Builder
    @Getter
    @NoArgsConstructor // 기본 생성자 초기화
    @AllArgsConstructor // 모든 필드 초기화
    public static class VerifyCodeRequest {
        private String email;
        private String code;
    }

}
