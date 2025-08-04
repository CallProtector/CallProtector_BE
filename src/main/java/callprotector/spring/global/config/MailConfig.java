package callprotector.spring.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // 환경변수에서 값 읽기
        String username = System.getenv("MAIL_USERNAME");
        String password = System.getenv("MAIL_PASSWORD");


        // 메일 서버 설정 (Gmail)
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername(username);       // Gmail 주소
        mailSender.setPassword(password);        // 앱 비밀번호

        // 추가 속성 설정
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");  // 개발 시 로그 확인용

        return mailSender;
    }
}

