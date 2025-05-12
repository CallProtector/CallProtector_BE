package callprotector.spring.web.dto.request;

import lombok.*;

public class UserRequestDTO {

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
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
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    // 로그인 요청 DTO
    public static class LoginDTO {
        private String email;
        private String password;

    }

}
