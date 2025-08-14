package callprotector.spring.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import callprotector.spring.global.annotation.UserId;
import callprotector.spring.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Test", description = "@UserId 커스텀 어노테이션 관련 테스트 API")
public class TestController {

	@Operation(
			summary = "인증된 사용자 ID 조회 API",
			description = "@UserId 커스텀 어노테이션이 올바르게 인증 정보를 주입하는지 확인하기 위한 테스트용 API입니다."
	)
	@GetMapping("/api/test/my-info")
	public ApiResponse<String> getMyInfo(@UserId Long userId) {
		log.info("인증된 사용자의 ID: {}", userId);
		return ApiResponse.onSuccess("User ID " + userId + "님의 정보를 성공적으로 조회했습니다.");
	}

}
