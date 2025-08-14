package callprotector.spring.global.twilio.controller;

import callprotector.spring.global.annotation.UserId;
import callprotector.spring.global.apiPayload.ApiResponse;
import callprotector.spring.global.twilio.dto.TwilioTokenResponseDTO;

import com.twilio.jwt.accesstoken.AccessToken;
import com.twilio.jwt.accesstoken.VoiceGrant;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/token")
@Tag(
        name = "TwilioVoiceToken",
        description = "Twilio 음성 통화를 위한 액세스 토큰 발급 API. 브라우저 클라이언트가 Twilio Voice SDK를 사용해 전화를 수·발신할 수 있도록 토큰을 발급합니다."
)
public class TwilioVoiceTokenController {
    @Value("${twilio.account.sid}")
    private String TWILIO_ACCOUNT_SID;

    @Value("${twilio.api.key}")
    private String TWILIO_API_KEY;

    @Value("${twilio.api.secret}")
    private String TWILIO_API_SECRET;

    @GetMapping
    public ApiResponse<TwilioTokenResponseDTO> getToken(
        @UserId Long userId
    ){
        String fixedIdentity = "browserUser"; // 고정 ID - 단일 사용자로 구현, 추후 상담사 분배 시에 다중 사용자로 확장 가능
        VoiceGrant grant = new VoiceGrant();
        grant.setIncomingAllow(true);

        AccessToken token = new AccessToken.Builder(
                TWILIO_ACCOUNT_SID,
                TWILIO_API_KEY,
                TWILIO_API_SECRET
        )
                .identity(fixedIdentity)
                .grant(grant)
                .ttl(3600)
                .build();

        log.info("access token: {}", token.toString());
        log.info("userId : {}", userId);

        TwilioTokenResponseDTO twilioTokenResponseDTO = TwilioTokenResponseDTO.of(token.toJwt(),  userId);
        return ApiResponse.onSuccess(twilioTokenResponseDTO);
    }
}
