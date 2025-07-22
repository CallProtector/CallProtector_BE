package callprotector.spring.service.UserService;

import callprotector.spring.web.dto.request.UserRequestDTO;
import callprotector.spring.web.dto.response.UserResponseDTO;

public interface UserService {

    public UserResponseDTO.SignupDTO create(final UserRequestDTO.SignupDTO dto);
    public UserResponseDTO.LoginDTO login(final UserRequestDTO.LoginDTO dto);

    public UserResponseDTO.checkEmailDTO checkEmail(final String email);

    public void verifyEmail(String token);

}
