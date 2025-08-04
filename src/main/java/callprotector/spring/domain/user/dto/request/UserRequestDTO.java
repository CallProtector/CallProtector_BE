package callprotector.spring.domain.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserRequestDTO {

    @Builder
    @Getter
    @NoArgsConstructor // 기본 생성자 초기화
    @AllArgsConstructor // 모든 필드 초기화
    // 회원가입 요청 DTO
    public static class SignupDTO {
        private String name;
        private String email;
        private String password;
        private String phone;
        // ! 프로필 이미지, 직책, 부서는 아직 안 넣음 (API 명세서와 피그마 화면에 없기 때문) !
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    // 로그인 요청 DTO
    public static class LoginDTO {
        private String email;
        private String password;

    }

}
