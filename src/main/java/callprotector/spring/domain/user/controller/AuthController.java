package callprotector.spring.domain.user.controller;

import callprotector.spring.global.apiPayload.ApiResponse;
import callprotector.spring.domain.user.service.UserService;
import callprotector.spring.domain.user.dto.request.UserRequestDTO;
import callprotector.spring.domain.user.dto.request.VerifyCodeRequestDTO;
import callprotector.spring.domain.user.dto.response.UserResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "유저 관련 API")
public class AuthController {

    private final UserService userService;

    @Operation(summary = "인증 코드 전송 API", description = "요청한 이메일 주소로 인증 코드를 전송합니다.")
    @PostMapping("/send-code")
    public ApiResponse<String> sendVerificationCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        userService.sendVerificationCode(email);
        return ApiResponse.onSuccess("인증 코드가 이메일로 전송되었습니다.");
    }

    @Operation(summary = "인증 코드 검증 API", description = "이메일과 수신한 인증 코드를 검증하여 이메일 인증을 완료합니다.")
    @PostMapping("/verify-code")
    public ApiResponse<String> verifyCode(@RequestBody VerifyCodeRequestDTO.VerifyCodeRequest dto) {
        userService.verifyCode(dto.getEmail(), dto.getCode());
        return ApiResponse.onSuccess("이메일 인증이 완료되었습니다.");
    }


    @Operation(summary = "회원가입 API",
            description = "이름, 이메일, 전화번호, 비밀번호를 받아 회원을 생성합니다. " +
                            "비밀번호는 영문·숫자·특수문자 포함 8~20자입니다.")
    @PostMapping("/signup")
    public ApiResponse<UserResponseDTO.SignupDTO> signup(@RequestBody UserRequestDTO.SignupDTO user){
        UserResponseDTO.SignupDTO result = userService.create(user);
        return ApiResponse.onSuccess(result);
    }

    @Operation(summary = "로그인 API", description = "이메일과 비밀번호로 로그인하고 액세스 토큰 등 인증 정보를 반환합니다.")
    @PostMapping("/login")
    public ApiResponse<UserResponseDTO.LoginDTO> login(@RequestBody UserRequestDTO.LoginDTO loginDTO){
        UserResponseDTO.LoginDTO result = userService.login(loginDTO);
        return ApiResponse.onSuccess(result);
    }
}
