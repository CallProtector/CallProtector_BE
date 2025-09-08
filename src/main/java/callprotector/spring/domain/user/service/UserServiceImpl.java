package callprotector.spring.domain.user.service;

import callprotector.spring.global.apiPayload.code.status.ErrorStatus;
import callprotector.spring.global.apiPayload.exception.handler.*;
import callprotector.spring.domain.user.entity.User;
import callprotector.spring.domain.user.entity.VerificationToken;
import callprotector.spring.domain.user.repository.UserRepository;
import callprotector.spring.domain.user.repository.VerificationTokenRepository;
import callprotector.spring.global.security.TokenProvider;
import callprotector.spring.global.util.PasswordValidator;
import callprotector.spring.domain.user.dto.request.UserRequestDTO;
import callprotector.spring.domain.user.dto.response.UserResponseDTO;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;

    private final PasswordEncoder passwordEncoder; // WebSecurityConfig에서 @Bean으로 설정해놓아서, 주입하기만 하면 됨
    
    // 이메일 인증
    private final VerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    // 이메일 인증 코드 발송
    @Override
    @Transactional
    public void sendVerificationCode(String email) {
        // 이미 가입된 이메일인지 확인
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            throw new MailGeneralException(ErrorStatus.MAIL_ALREADY_EXISTS);
        }

        String code = String.format("%06d", new Random().nextInt(999999));
        VerificationToken token = VerificationToken.create(email, code);
        tokenRepository.save(token);

        emailService.sendVerificationEmail(email, code);
    }

    // 인증 코드 검증
    @Override
    @Transactional
    public void verifyCode(String email, String code) {
        // 인증 요청 자체가 없는 경우
        VerificationToken token = tokenRepository.findTopByEmailOrderByExpiresAtDesc(email)
                .orElseThrow(() -> new MailGeneralException(ErrorStatus.VERIFICATION_NOT_FOUND));
        // 인증 코드 만료된 경우
        if (token.isExpired()) {
            throw new MailGeneralException(ErrorStatus.VERIFICATION_CODE_EXPIRED);
        }
        // 인증 코드 불일치
        if (!token.getCode().equals(code)) {
            throw new MailGeneralException(ErrorStatus.VERIFICATION_CODE_INVALID);
        }
        // 인증 성공 처리
        token.markVerified(); // verified = true 로 표시
    }

    // 회원가입
    @Override
    @Transactional
    public UserResponseDTO.SignupDTO create(UserRequestDTO.SignupDTO dto) {

        // 1. 이메일 인증 완료 여부 확인
        VerificationToken token = tokenRepository.findTopByEmailOrderByExpiresAtDesc(dto.getEmail())
                .orElseThrow(() -> new AuthGeneralException(ErrorStatus.EMAIL_NOT_VERIFIED));

        if (!token.isVerified()) {
            throw new AuthGeneralException(ErrorStatus.EMAIL_NOT_VERIFIED);
        }

        // 2. 비밀번호 유효성 검사
        if (!PasswordValidator.isValid(dto.getPassword())) {
            throw new AuthGeneralException(ErrorStatus.PASSWORD_POLICY_VIOLATION);
        }

        // 3. User 저장
        User user = User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .phoneNumber(String.valueOf(dto.getPhone()))
                .build();

        User savedUser = userRepository.save(user);

        // 4. 응답 반환
        return UserResponseDTO.SignupDTO.builder()
                .id(savedUser.getId())
                .build();
    }

    // 로그인
    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO.LoginDTO login(UserRequestDTO.LoginDTO dto) {
        User user = getUserByEmail(dto.getEmail());

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new PasswordMismatchException();
        }

        return UserResponseDTO.LoginDTO.builder()
                .token(tokenProvider.create(user))
                .id(user.getId())
                .name(user.getName())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(UserNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(UserValidationException::new);
    }
}
