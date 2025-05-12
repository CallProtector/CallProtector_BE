package callprotector.spring.web.dto.response;

import lombok.*;

public class UserResponseDTO {

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignupDTO{
        Long id; // 사용자 id값
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginDTO{
        String token;
        Long id;
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class checkEmailDTO{
        boolean available;
    }

}
