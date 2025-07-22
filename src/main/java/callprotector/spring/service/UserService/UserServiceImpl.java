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

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;

    private final PasswordEncoder passwordEncoder; // WebSecurityConfig에서 @Bean으로 설정해놓아서, 주입하기만 하면 됨
    
    // 이메일 인증
    private final VerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    // 회원가입
    @Override
    public UserResponseDTO.SignupDTO create(UserRequestDTO.SignupDTO dto) {
        // 비밀번호 유효성 검사
        if (!PasswordValidator.isValid(dto.getPassword())) {
            throw new IllegalArgumentException("비밀번호는 8~16자이며, 영문, 숫자, 특수문자를 모두 포함해야 합니다.");
        }


        // 아직 에러처리.. 중복처리는 하지 않았어요. 정말 기본만 !
        User user = User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .phoneNumber(Long.valueOf(dto.getPhone()))
                .build();
        // 2. 저장
        User savedUser = userRepository.save(user);

        // 3. 이메일 인증 토큰 생성 및 저장
        VerificationToken token = VerificationToken.createToken(savedUser);
        tokenRepository.save(token);

        // 4. 인증 이메일 발송
        emailService.sendVerificationEmail(savedUser.getEmail(), token.getToken());

        // 5. 응답
        return UserResponseDTO.SignupDTO.builder()
                .id(savedUser.getId())
                .build();
    }

    // 로그인
    @Override
    public UserResponseDTO.LoginDTO login(UserRequestDTO.LoginDTO dto) {
        final Optional<User> user = userRepository.findByEmail(dto.getEmail());
        if (!user.get().isVerified()) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
        }
        if (user.isPresent() && passwordEncoder.matches(dto.getPassword(), user.get().getPassword())){
            return UserResponseDTO.LoginDTO.builder().token(tokenProvider.create(user.get()))
                    .id(user.get().getId()).build();
        } else {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
    }

    @Override
    public UserResponseDTO.checkEmailDTO checkEmail(String email) {
        final Optional<User> user = userRepository.findByEmail(email);
        if(user.isEmpty()){
            return UserResponseDTO.checkEmailDTO.builder().available(true).build();
        } else {
            return UserResponseDTO.checkEmailDTO.builder().available(false).build();
        }
    }

    @Transactional
    @Override
    public void verifyEmail(String token) {
        Optional<VerificationToken> optionalToken = tokenRepository.findByToken(token);

        if (optionalToken.isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        VerificationToken verificationToken = optionalToken.get();

        // 토큰 만료 검사 주석 처리
//        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
//            throw new IllegalArgumentException("토큰이 만료되었습니다.");
//        }


        User user = verificationToken.getUser();

        if (user.isVerified()) {
            throw new IllegalArgumentException("이미 인증된 사용자입니다.");
        }

        user.verify();
        userRepository.save(user);
    }
}
