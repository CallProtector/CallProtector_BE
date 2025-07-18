package callprotector.spring.web.controller;

import callprotector.spring.apiPayload.ApiResponse;
import callprotector.spring.web.dto.response.TwilioTokenResponseDTO;

import com.twilio.jwt.accesstoken.AccessToken;
import com.twilio.jwt.accesstoken.VoiceGrant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/token")
public class TwilioVoiceTokenController {


    @Value("${twilio.account.sid}")
    private String TWILIO_ACCOUNT_SID;

    @Value("${twilio.api.key}")
    private String TWILIO_API_KEY;

    @Value("${twilio.api.secret}")
    private String TWILIO_API_SECRET;

    @GetMapping
    public ApiResponse<TwilioTokenResponseDTO> getToken(){
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

        System.out.println("access token: " + token.toString());

        TwilioTokenResponseDTO twilioTokenResponseDTO = TwilioTokenResponseDTO.of(token.toJwt());
        return ApiResponse.onSuccess(twilioTokenResponseDTO);
    }
}
