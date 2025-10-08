package callprotector.spring.global.twilio.controller;

import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/twilio")
@Tag(
        name = "TwilioWebhook",
        description = "Twilio 음성 통화 Webhook API. 인바운드 콜 수신 시 브라우저 클라이언트와 WebSocket을 연결하고 통화 흐름(TwiML)을 제어합니다."
)
public class TwilioWebhookController {
    private static final String BROWSER_CLIENT_ID = "browserUser";
    private static final String WS_URL ="wss://callprotect.site/ws/audio";

    @PostMapping(value = "/voice", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String onIncomingCall(@RequestParam Map<String, String> params) {
        log.info("params: {}", params);
        log.info("☆ Inbound Call - From: {}", params.get("From"));
        log.info("☆ Inbound Call - CallSid: {}", params.get("CallSid"));
        String callerNumber = params.get("From");
        String inboundCallSid = params.get("CallSid");

        Client clientVerb = new Client.Builder(BROWSER_CLIENT_ID) // 브라우저 Client ID - TwilioVoiceTokenController의 fixedIdentity와 일치해야 함
            .parameter(new Parameter.Builder()
                .name("initialCallSid")
                .value(inboundCallSid)
                .build())
            .build();

        VoiceResponse response = new VoiceResponse.Builder()
                .say(new Say.Builder("산업안전보건법에 따라 고객응대 근로자 보호 조치가 시행되고 있습니다." +
                        "세 번 이상 폭언이 감지되면 에이아이에 의해 통화가 자동 종료됩니다.")
                    .voice(Say.Voice.ALICE)
                    .language(Say.Language.KO_KR)
                    .build())
                .start(new Start.Builder()
                        .stream(new Stream.Builder()
                                .url(WS_URL)
                                .track(Stream.Track.BOTH_TRACKS)
                                .parameter(new com.twilio.twiml.voice.Parameter.Builder()
                                    .name("primaryCallSid")
                                    .value(inboundCallSid)
                                        .build())
                                .parameter(new com.twilio.twiml.voice.Parameter.Builder()
                                        .name("callerNumber")
                                        .value(callerNumber)
                                        .build())
                                .build())
                        .build())
                .dial(new Dial.Builder()
                        .timeout(30) // 30초 안에 응답 없으면 통화 불가 안내 멘트
                        .client(clientVerb)
                        .build())
                .say(new Say.Builder("지금은 통화가 불가능한 시간입니다. 나중에 다시 걸어주세요.") // 안내 멘트 수정 예정
                        .voice(Say.Voice.ALICE)
                        .language(Say.Language.KO_KR)
                        .build())

                .build();

        return response.toXml(); // TwiML 반환
    }
}