package callprotector.spring.web.controller;

import callprotector.spring.apiPayload.ApiResponse;
import callprotector.spring.domain.User;
import callprotector.spring.domain.VerificationToken;
import callprotector.spring.repository.UserRepository;
import callprotector.spring.repository.VerificationTokenRepository;
import callprotector.spring.service.UserService.UserService;
import callprotector.spring.web.dto.request.UserRequestDTO;
import callprotector.spring.web.dto.response.UserResponseDTO;
import com.google.common.base.Optional;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class AuthController {

    private final UserService userService;


    // 회원가입 API
    @Operation(summary = "회원가입 API", description = "회원가입 API입니다.")
    @PostMapping("/auth/signup")
    public ApiResponse<UserResponseDTO.SignupDTO> signup(@RequestBody UserRequestDTO.SignupDTO user){
        // 이거 ResponseDTO로 백퍼 수정해야됨
        UserResponseDTO.SignupDTO result = userService.create(user);
        return ApiResponse.onSuccess(result);
    }

    // 로그인 API
    @Operation(summary = "로그인 API", description = "로그인 API입니다.")
    @PostMapping("/auth/login")
    public ApiResponse<UserResponseDTO.LoginDTO> login(@RequestBody UserRequestDTO.LoginDTO loginDTO){
        // 이거 ResponseDTO로 백퍼 수정해야됨
        UserResponseDTO.LoginDTO result = userService.login(loginDTO);
        return ApiResponse.onSuccess(result);
    }

    // 이메일 중복 확인 API <- 지금 당장 필요하지 않아, 응답 수정 X
    @GetMapping("/auth/checkEmail")
    public ResponseEntity<?> checkEmail(@RequestParam String email){
        UserResponseDTO.checkEmailDTO result = userService.checkEmail(email);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/auth/verify-email")
    public ApiResponse<String> verifyEmail(@RequestParam("token") String token) {
        userService.verifyEmail(token);
        return ApiResponse.onSuccess("이메일 인증이 완료되었습니다.");
    }










}
