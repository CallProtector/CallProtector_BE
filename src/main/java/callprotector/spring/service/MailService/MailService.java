package callprotector.spring.service.MailService;

import callprotector.spring.web.dto.response.MailDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


public interface MailService {
    public MailDTO.CertificationNumber sendCertificationNumberEmail(String email);
}
