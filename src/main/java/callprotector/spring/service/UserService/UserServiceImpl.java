package callprotector.spring.service.UserService;

import callprotector.spring.domain.User;
import callprotector.spring.domain.VerificationToken;
import callprotector.spring.repository.UserRepository;
import callprotector.spring.repository.VerificationTokenRepository;
import callprotector.spring.security.TokenProvider;
import callprotector.spring.service.EmailService.EmailService;
import callprotector.spring.service.util.PasswordValidator;
import callprotector.spring.web.dto.request.UserRequestDTO;
import callprotector.spring.web.dto.response.UserResponseDTO;
import jakarta.transaction.Transactional;
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
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
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
        VerificationToken token = tokenRepository.findTopByEmailOrderByExpiresAtDesc(email)
                .orElseThrow(() -> new IllegalArgumentException("인증 요청이 없습니다."));

        if (token.isExpired()) {
            throw new IllegalArgumentException("인증 코드가 만료되었습니다.");
        }

        if (!token.getCode().equals(code)) {
            throw new IllegalArgumentException("인증 코드가 올바르지 않습니다.");
        }

        // ✅ 인증 성공 처리
        token.markVerified(); // verified = true 로 표시
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자"));
    }


    // 회원가입
    @Override
    public UserResponseDTO.SignupDTO create(UserRequestDTO.SignupDTO dto) {
        // 1. 비밀번호 유효성 검사
        if (!PasswordValidator.isValid(dto.getPassword())) {
            throw new IllegalArgumentException("비밀번호는 8~16자이며, 영문, 숫자, 특수문자를 모두 포함해야 합니다.");
        }

        // 2. 이메일 인증 완료 여부 확인
        VerificationToken token = tokenRepository.findTopByEmailOrderByExpiresAtDesc(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 인증이 필요합니다."));

        if (!token.isVerified()) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
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
    public UserResponseDTO.LoginDTO login(UserRequestDTO.LoginDTO dto) {
        final Optional<User> user = userRepository.findByEmail(dto.getEmail());

        if (user.isEmpty() || !passwordEncoder.matches(dto.getPassword(), user.get().getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return UserResponseDTO.LoginDTO.builder()
                .token(tokenProvider.create(user.get()))
                .id(user.get().getId())
                .build();
    }

}
