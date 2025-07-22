package callprotector.spring.service.EmailService;

public interface EmailService {
    public void sendVerificationEmail(String toEmail, String token);
}
