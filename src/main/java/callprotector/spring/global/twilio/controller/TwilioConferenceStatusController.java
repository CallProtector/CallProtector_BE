package callprotector.spring.global.twilio.controller;

import callprotector.spring.domain.callsession.service.CallSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/twilio")
public class TwilioConferenceStatusController {

    private final CallSessionService callSessionService;

    //Twilio → 서버로 Conference 상태 변경 Webhook 전송
    @PostMapping("/conference-status")
    public void handleConferenceStatus(@RequestParam Map<String, String> params) {
        String conferenceSid = params.get("ConferenceSid");
        String callSid = params.get("CallSid");
        String event = params.get("StatusCallbackEvent");

        log.info("📩 Conference Status 수신: event={}, callSid={}, conferenceSid={}",
                event, callSid, conferenceSid);

        if (conferenceSid == null || callSid == null) {
            log.warn("유효하지 않은 요청: params={}", params);
            return;
        }

        // 컨퍼런스 생성 또는 참가 시 DB에 저장
        if ("conference-start".equals(event) || "participant-join".equals(event)) {
            try {
                callSessionService.updateConferenceSidByCallSid(callSid, conferenceSid);
                log.info("✅ DB에 ConferenceSid 저장 완료 (CallSid={}, ConferenceSid={})", callSid, conferenceSid);
            } catch (Exception e) {
                log.error("❌ ConferenceSid 저장 실패: {}", e.getMessage(), e);
            }
        }

        if ("conference-end".equals(event) || "participant-leave".equals(event)) {
            log.info("컨퍼런스 종료 (ConferenceSid={})", conferenceSid);
        }
    }
}
