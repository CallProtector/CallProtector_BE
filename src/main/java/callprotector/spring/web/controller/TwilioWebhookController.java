package callprotector.spring.web.controller;

import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Client;
import com.twilio.twiml.voice.Dial;
import com.twilio.twiml.voice.Start;
import com.twilio.twiml.voice.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/twilio")
public class TwilioWebhookController {

    @PostMapping(value = "/voice", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String onIncomingCall(@RequestParam Map<String, String> params) {
        System.out.println("☆ Inbound Call From: " + params.get("From"));
        VoiceResponse response = new VoiceResponse.Builder()
                .start(new Start.Builder()
                        .stream(new Stream.Builder()
                                .url("wss://pet-pipefish-friendly.ngrok-free.app/ws/audio")
                                .track(Stream.Track.BOTH_TRACKS)
                                .build())
                        .build())
                .dial(new Dial.Builder()
                        .client(new Client.Builder("browserUser").build())  // 브라우저 Client ID - TwilioVoiceTokenController의 fixedIdentity와 일치해야 함
                        .build())
                .build();

        return response.toXml(); // TwiML 반환
    }
}