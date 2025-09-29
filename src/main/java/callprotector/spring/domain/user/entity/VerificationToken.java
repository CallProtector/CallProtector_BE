package callprotector.spring.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class VerificationToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String code;

    private LocalDateTime expiresAt;

    private boolean verified; // 인증 완료 여부 저장

    public static VerificationToken create(String email, String code) {
        VerificationToken token = new VerificationToken();
        token.email = email;
        token.code = code;
        token.expiresAt = LocalDateTime.now().plusMinutes(10); // 예: 10분 유효
        return token;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    public void markVerified() {
        this.verified = true;
    }
}

