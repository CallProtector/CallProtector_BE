package callprotector.spring.domain.user.service;

public interface EmailService {
    public void sendVerificationEmail(String toEmail, String token);
}
