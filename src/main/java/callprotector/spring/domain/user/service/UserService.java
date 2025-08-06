package callprotector.spring.domain.user.service;

import callprotector.spring.domain.user.entity.User;
import callprotector.spring.domain.user.dto.request.UserRequestDTO;
import callprotector.spring.domain.user.dto.response.UserResponseDTO;

public interface UserService {

    public UserResponseDTO.SignupDTO create(final UserRequestDTO.SignupDTO dto);

    public UserResponseDTO.LoginDTO login(final UserRequestDTO.LoginDTO dto);

    public void sendVerificationCode(String email);

    public void verifyCode(String email, String code);

    public User getUserById(Long id);

    public User getUserByEmail(String email);
}
