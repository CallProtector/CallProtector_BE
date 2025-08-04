package callprotector.spring.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import callprotector.spring.global.annotation.UserId;
import callprotector.spring.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
public class TestController {

	@GetMapping("/api/test/my-info")
	public ApiResponse<String> getMyInfo(@UserId Long userId) {
		log.info("인증된 사용자의 ID: {}", userId);
		return ApiResponse.onSuccess("User ID " + userId + "님의 정보를 성공적으로 조회했습니다.");
	}
}
