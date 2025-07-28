package callprotector.spring.web.controller;

import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/twilio")
public class TwilioWebhookController {

    @PostMapping(value = "/voice", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String onIncomingCall(@RequestParam Map<String, String> params) {
        log.info("☆ Inbound Call From: {}", params.get("From"));
        String callerNumber = params.get("From");

        String userId = "1"; // 추후 수정 필요(로그인한 상담원의 userId 가져오기)

        VoiceResponse response = new VoiceResponse.Builder() // 사전 안내 멘트 추가 예정
                .start(new Start.Builder()
                        .stream(new Stream.Builder()
                                .url("wss://callprotect.site/ws/audio")
                                .track(Stream.Track.BOTH_TRACKS)
                                .parameter(new com.twilio.twiml.voice.Parameter.Builder()
                                        .name("userId")
                                        .value(userId)
                                        .build())
                                .parameter(new com.twilio.twiml.voice.Parameter.Builder()
                                        .name("callerNumber")
                                        .value(callerNumber)
                                        .build())
                                .build())
                        .build())
                .dial(new Dial.Builder()
                        .timeout(30) // 30초 안에 응답 없으면 통화 불가 안내 멘트
                        .client(new Client.Builder("browserUser").build())  // 브라우저 Client ID - TwilioVoiceTokenController의 fixedIdentity와 일치해야 함
                        .build())
                .say(new Say.Builder("지금은 통화가 불가능한 시간입니다. 나중에 다시 걸어주세요.") // 안내 멘트 수정 예정
                        .voice(Say.Voice.ALICE)
                        .language(Say.Language.KO_KR)
                        .build())
                .build();

        return response.toXml(); // TwiML 반환
    }
}