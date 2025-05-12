package callprotector.spring.web.controller;

import callprotector.spring.service.UserService.UserService;
import callprotector.spring.web.dto.request.UserRequestDTO;
import callprotector.spring.web.dto.response.UserResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class AuthController {

    private final UserService userService;

    // 회원가입 API
    @PostMapping("/auth/signup")
    public ResponseEntity<?> signup(@RequestBody UserRequestDTO.SignupDTO user){
        // 이거 ResponseDTO로 백퍼 수정해야됨
        UserResponseDTO.SignupDTO result = userService.create(user);
        return ResponseEntity.ok(result);
    }

    // 로그인 API
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody UserRequestDTO.LoginDTO loginDTO){
        // 이거 ResponseDTO로 백퍼 수정해야됨
        UserResponseDTO.LoginDTO result = userService.login(loginDTO);
        return ResponseEntity.ok(result);
    }

    // 이메일 중복 확인 API
    @GetMapping("/auth/checkEmail")
    public ResponseEntity<?> checkEmail(@RequestParam String email){
        UserResponseDTO.checkEmailDTO result = userService.checkEmail(email);
        return ResponseEntity.ok(result);
    }









}
