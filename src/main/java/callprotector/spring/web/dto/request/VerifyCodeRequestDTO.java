package callprotector.spring.web.dto.request;

import lombok.*;

public class VerifyCodeRequestDTO {
    @Builder
    @Getter
    @Setter
    @NoArgsConstructor // 기본 생성자 초기화
    @AllArgsConstructor // 모든 필드 초기화
    public static class VerifyCodeRequest {
        private String email;
        private String code;
    }

}
