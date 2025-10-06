package callprotector.spring.global.twilio.controller;

import com.twilio.http.HttpMethod;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.*;
import com.twilio.type.PhoneNumber;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Arrays;
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

    private final TwilioRestClient twilioClient;

    private static final String BROWSER_CLIENT_ID = "browserUser";
    private static final String NGROK_HOST = "pet-pipefish-friendly.ngrok-free.app";
    private static final String WS_URL = "wss://" + NGROK_HOST + "/ws/audio";
    private static final String STATUS_CALLBACK_URL = "https://" + NGROK_HOST + "/twilio/conference-status";
    private static final String AGENT_TWIML_URL = "https://" + NGROK_HOST + "/twilio/agent-twiML";

    /**
     * 인바운드 콜 수신 시 고객을 Conference에 연결하고, 상담원에게 REST API로 아웃바운드 콜을 겁니다.
     */
    @PostMapping(value = "/voice", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String onIncomingCall(@RequestParam Map<String, String> params) {
        log.info("params: {}", params);
        log.info("☆ Inbound Call - From: {}", params.get("From"));
        log.info("☆ Inbound Call - CallSid: {}", params.get("CallSid"));

        String callerNumber = params.get("From");
        String twilioNumber = params.get("To");
        String inboundCallSid = params.get("CallSid");
        String conferenceName = inboundCallSid; // Conference 이름으로 Inbound CallSid 사용

        // 1. STT 실시간 Stream 연결
        Start stream = new Start.Builder()
                .stream(new Stream.Builder()
                        .url(WS_URL)
                        .track(Stream.Track.BOTH_TRACKS)
                        .parameter(new Parameter.Builder().name("primaryCallSid").value(inboundCallSid).build())
                        .parameter(new Parameter.Builder().name("callerNumber").value(callerNumber).build())
                        .build())
                .build();

        // 2. 고객을 Conference에 연결
        Conference customerConference = new Conference.Builder(conferenceName)
                .startConferenceOnEnter(true)
                .endConferenceOnExit(false)
                .statusCallback(STATUS_CALLBACK_URL)
                .statusCallbackEvents(Arrays.asList(
                        Conference.Event.START,
                        Conference.Event.END,
                        Conference.Event.JOIN,
                        Conference.Event.LEAVE))
                .build();

        Dial dial = new Dial.Builder().conference(customerConference).build();

        VoiceResponse response = new VoiceResponse.Builder()
                .say(new Say.Builder("테스트")
                        .voice(Say.Voice.ALICE)
                        .language(Say.Language.KO_KR).build())
                .start(stream)
                .dial(dial)
                .build();

        // 3. 서버에서 상담원에게 아웃바운드 콜을 거는 REST API 호출
        try {
            String fullAgentTwimlUrl = AGENT_TWIML_URL + "?ConferenceName=" + conferenceName;

            Call agentCall = Call.creator(
                            new com.twilio.type.Client(BROWSER_CLIENT_ID),  // 상담원
                            new PhoneNumber(twilioNumber),                  // 발신자
                            new URI(fullAgentTwimlUrl)                      // TwiML URL 교체
                    )
                    .setMachineDetection("Enable")
                    .setMethod(HttpMethod.POST)
                    .setStatusCallback("https://pet-pipefish-friendly.ngrok-free.app/twilio/agent-callback")
                    .create(twilioClient);

            log.info("📞 Outbound Call to Agent Initiated. Agent CallSid: {}", agentCall.getSid());

        } catch (Exception e) {
            log.error("🚨 상담원에게 전화를 거는 중 오류 발생.", e);
        }

        log.info("📡 TwiML XML (고객 Conference Join) :\n{}", response.toXml());
        return response.toXml();
    }

    /**
     * 상담원에게 아웃바운드 콜이 걸린 후, 상담원이 전화를 수락하면 실행되는 TwiML입니다.
     */
    @PostMapping(value = "/agent-twiML", produces = MediaType.APPLICATION_XML_VALUE)
    public String agentTwiML(@RequestParam Map<String, String> params) {
        String conferenceName = params.get("ConferenceName");

        if (conferenceName == null || conferenceName.isBlank()) {
            log.error("🚨 Agent TwiML 호출 시 ConferenceName 누락");
        }

        log.info("☆ Agent Answered - ConferenceName={}", conferenceName);

        // 상담원도 동일한 Conference에 Join
        Conference agentConference = new Conference.Builder(conferenceName)
                .startConferenceOnEnter(true)
                .endConferenceOnExit(true)
                .build();

        Dial dial = new Dial.Builder().conference(agentConference).build();

        VoiceResponse response = new VoiceResponse.Builder()
                .say(new Say.Builder("상담원 연결 완료")
                        .language(Say.Language.KO_KR)
                        .voice(Say.Voice.ALICE)
                        .build())
                .dial(dial)
                .build();

        log.info("📡 TwiML XML (상담원 Join) :\n{}", response.toXml());

        return response.toXml();
    }
}