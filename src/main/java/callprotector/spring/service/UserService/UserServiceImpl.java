package callprotector.spring.service.UserService;

import callprotector.spring.domain.User;
import callprotector.spring.repository.UserRepository;
import callprotector.spring.security.TokenProvider;
import callprotector.spring.web.dto.request.UserRequestDTO;
import callprotector.spring.web.dto.response.UserResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;

    private final PasswordEncoder passwordEncoder; // WebSecurityConfig에서 @Bean으로 설정해놓아서, 주입하기만 하면 됨


    @Override
    public UserResponseDTO.SignupDTO create(UserRequestDTO.SignupDTO dto) {
        // 아직 에러처리.. 중복처리는 하지 않았어요. 정말 기본만 !
        User user = User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .phoneNumber(Long.valueOf(dto.getPhone()))
                .build();
        User result = userRepository.save(user);

        return UserResponseDTO.SignupDTO.builder().id(result.getId()).build();
    }

    @Override
    public UserResponseDTO.LoginDTO login(UserRequestDTO.LoginDTO dto) {
        final Optional<User> user = userRepository.findByEmail(dto.getEmail());
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
}
