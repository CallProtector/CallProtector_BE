package callprotector.spring.web.controller;

import callprotector.spring.global.apiPayload.ApiResponse;
import callprotector.spring.service.UserService.UserService;
import callprotector.spring.web.dto.request.UserRequestDTO;
import callprotector.spring.web.dto.request.VerifyCodeRequestDTO;
import callprotector.spring.web.dto.response.UserResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    // 메일 인증 코드 보내는 api
    @Operation(summary = "인증코드 보내는 API", description = "설정한 값에 해당하는 메일을 확인해 주세요.")
    @PostMapping("/send-code")
    public ApiResponse<String> sendVerificationCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        userService.sendVerificationCode(email);
        return ApiResponse.onSuccess("인증 코드가 이메일로 전송되었습니다.");
    }

    // 인증 코드 검증하는 api
    @Operation(summary = "인증코드 검증하는 API", description = "1. 메일 2. 받은 인증코드 값을 넣어주세요.")
    @PostMapping("/verify-code")
    public ApiResponse<String> verifyCode(@RequestBody VerifyCodeRequestDTO.VerifyCodeRequest dto) {
        userService.verifyCode(dto.getEmail(), dto.getCode());
        return ApiResponse.onSuccess("이메일 인증이 완료되었습니다.");
    }



    // 회원가입 API
    @Operation(summary = "회원가입 API", description = "이름, 메일, 전화번호, 비밀번호를 넣어주세요. 비밀번호는 영문, 숫자, 특수문자 포함 8~20자리입니다.")
    @PostMapping("/signup")
    public ApiResponse<UserResponseDTO.SignupDTO> signup(@RequestBody UserRequestDTO.SignupDTO user){
        // 이거 ResponseDTO로 백퍼 수정해야됨
        UserResponseDTO.SignupDTO result = userService.create(user);
        return ApiResponse.onSuccess(result);
    }

    // 로그인 API
    @Operation(summary = "로그인 API", description = "메일, 비밀번호를 넣어주세요.")
    @PostMapping("/login")
    public ApiResponse<UserResponseDTO.LoginDTO> login(@RequestBody UserRequestDTO.LoginDTO loginDTO){
        // 이거 ResponseDTO로 백퍼 수정해야됨
        UserResponseDTO.LoginDTO result = userService.login(loginDTO);
        return ApiResponse.onSuccess(result);
    }










}
