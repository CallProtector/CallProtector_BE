package callprotector.spring.service.MailService;

import callprotector.spring.web.dto.response.MailDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService{

    @Autowired
    private JavaMailSender javaMailSender;

    @Override
    public MailDTO.CertificationNumber sendCertificationNumberEmail(String email) {
        Random random = new Random();
        // 다섯 자리 만들기 ??
        Integer randomNumber = random.nextInt(9000) + 1000;
        try{
            SimpleMailMessage message = new SimpleMailMessage();
            message.setSubject("[CallProtector] - 본인 인증");
            message.setTo(email);
            message.setText("인증 번호 : " +randomNumber);
            javaMailSender.send(message);
            return MailDTO.CertificationNumber.builder().number(randomNumber).build();
        } catch (Exception e){
            throw new IllegalArgumentException("본인 인증 번호가 올바르지 않습니다.");
        }
    }
}
