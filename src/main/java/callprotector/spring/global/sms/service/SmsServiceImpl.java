package callprotector.spring.global.sms.service;

import callprotector.spring.global.apiPayload.exception.handler.SmsSendException;
import com.solapi.sdk.message.dto.response.MultipleDetailMessageSentResponse;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.service.DefaultMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    private final DefaultMessageService messageService;

    @Value("${solapi.sender}")
    private String sender;

    @Override
    public MultipleDetailMessageSentResponse sendTerminationNotice(String rawTo, Set<String> abuseTypes) {
        String to = normalizePhone(rawTo);
        String from = normalizePhone(sender);

        String joinedTypes = String.join(", ", (abuseTypes == null || abuseTypes.isEmpty())
                ? Set.of("부적절한 발언") : abuseTypes);
        String text = "[온음] 폭언(" + joinedTypes + ") 3회 발생으로 인해 통화가 자동 종료되었습니다.";

        Message m = new Message();
        m.setFrom(from);
        m.setTo(to);
        m.setText(text);

        try {
            return messageService.send(m);
        } catch (Exception e) {
            throw new SmsSendException();
        }
    }

    private String normalizePhone(String s) {
        if (s == null) return null;
        String digits = s.replaceAll("\\D+", "");
        // +82로 시작하는 국제번호 정리: 82 → 0
        if (digits.startsWith("82")) {
            digits = "0" + digits.substring(2);
        }
        return digits;
    }

}