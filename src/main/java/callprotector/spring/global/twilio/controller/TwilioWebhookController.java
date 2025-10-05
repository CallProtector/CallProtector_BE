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
    private static final String NGROK_HOST = "pet-pipefish-friendly.ngrok-free.app"; // 현재 ngrok 주소
    private static final String WS_URL = "wss://" + NGROK_HOST + "/ws/audio";
    private static final String STATUS_CALLBACK_URL = "https://" + NGROK_HOST + "/twilio/conference-status";

    @PostMapping(value = "/voice", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String onIncomingCall(@RequestParam Map<String, String> params) {
        log.info("params: {}", params);
        log.info("☆ Inbound Call - From: {}", params.get("From"));
        log.info("☆ Inbound Call - CallSid: {}", params.get("CallSid"));
        String callerNumber = params.get("From");
        String inboundCallSid = params.get("CallSid");

        // STT 실시간 Stream 연결
        Start stream = new Start.Builder()
                .stream(new Stream.Builder()
                        .url(WS_URL)
                        .track(Stream.Track.BOTH_TRACKS)
                        .parameter(new Parameter.Builder().name("primaryCallSid").value(inboundCallSid).build())
                        .parameter(new Parameter.Builder().name("callerNumber").value(callerNumber).build())
                        .build())
                .build();

        // 상담원(Client) 연결 설정
        Client agentClient = new Client.Builder(BROWSER_CLIENT_ID)
                .parameter(new Parameter.Builder().name("initialCallSid").value(inboundCallSid).build())
                .url("https://" + NGROK_HOST + "/twilio/connect-agent-to-conference")
                .build();

        Dial agentDial = new Dial.Builder().client(agentClient).build();

        VoiceResponse response = new VoiceResponse.Builder()
                .say(new Say.Builder("테스트")
                        .voice(Say.Voice.ALICE)
                        .language(Say.Language.KO_KR).build())
                .start(stream)
                .dial(agentDial)
                .build();

        // 추가할 Conference XML
        String conferenceXml =
                "<Dial>" +
                        "<Conference startConferenceOnEnter=\"true\" " +
                        "endConferenceOnExit=\"false\" " +
                        "statusCallback=\"" + STATUS_CALLBACK_URL + "\" " +
                        "statusCallbackEvent=\"start end join leave\">" +
                        inboundCallSid +
                        "</Conference>" +
                        "</Dial>";

        // TwiML에 Conference 삽입
        String finalTwiml = response.toXml().replace("</Response>", conferenceXml + "</Response>");

        log.info("📡 TwiML XML :\n{}", finalTwiml);
        return finalTwiml;
    }

    @PostMapping(value = "/connect-agent-to-conference", produces = MediaType.APPLICATION_XML_VALUE)
    public String connectAgentToConference(@RequestParam Map<String, String> params) {
        String customerCallSid = params.get("initialCallSid");
        if (customerCallSid == null || customerCallSid.isBlank()) {
            customerCallSid = params.get("ParentCallSid");
        }

        log.info("☆ Agent Answered - ConferenceName={}", customerCallSid);

        VoiceResponse response = new VoiceResponse.Builder()
                .say(new Say.Builder("상담원 연결")
                        .language(Say.Language.KO_KR)
                        .voice(Say.Voice.ALICE)
                        .build())
                .build();

        String conferenceXml =
                "<Dial>" +
                        "<Conference startConferenceOnEnter=\"true\" endConferenceOnExit=\"true\">" +
                        customerCallSid +
                        "</Conference>" +
                        "</Dial>";

        String finalTwiml = response.toXml().replace("</Response>", conferenceXml + "</Response>");
        log.info("📡 TwiML XML (상담원 Join) :\n{}", finalTwiml);

        return finalTwiml;
    }
}