package callprotector.spring.service.UserService;

import callprotector.spring.domain.User;
import callprotector.spring.web.dto.request.UserRequestDTO;
import callprotector.spring.web.dto.response.UserResponseDTO;

public interface UserService {

    public UserResponseDTO.SignupDTO create(final UserRequestDTO.SignupDTO dto);
    public UserResponseDTO.LoginDTO login(final UserRequestDTO.LoginDTO dto);

    public void sendVerificationCode(String email);

    public void verifyCode(String email, String code);

    public User getUserByEmail(String email);

}
