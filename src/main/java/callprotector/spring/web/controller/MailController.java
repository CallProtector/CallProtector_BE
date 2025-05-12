package callprotector.spring.web.controller;

import callprotector.spring.service.MailService.MailService;
import callprotector.spring.web.dto.response.MailDTO;
import callprotector.spring.web.dto.response.UserResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mail")
public class MailController {
    private final MailService mailService;

    // 이메일 인증 API
    @GetMapping
    public ResponseEntity<?> mailSend(String email){
        MailDTO.CertificationNumber certificationNumber = mailService.sendCertificationNumberEmail(email);
        return ResponseEntity.ok(certificationNumber);
    }

    
}
