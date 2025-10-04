package callprotector.spring.global.twilio.service;

import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.conference.Participant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TwilioRestService {

    public void muteParticipantByConferenceSid(String conferenceSid,
                                               String participantCallSid,
                                               boolean mute) {
        try {
            Participant updated = Participant.updater(conferenceSid, participantCallSid)
                    .setMuted(mute)
                    .update();

            log.info("✅ 참가자 음소거/해제 성공: ConferenceSid={}, CallSid={}, Muted={}",
                    conferenceSid, updated.getCallSid(), updated.getMuted());
        } catch (ApiException e) {
            log.error("❌ Twilio API 요청 실패 (conferenceSid={}, participantCallSid={}, mute={})",
                    conferenceSid, participantCallSid, mute, e);
        } catch (Exception e) {
            log.error("❌ 알 수 없는 오류 발생", e);
        }
    }

}
