package callprotector.spring.global.sms.service;

import com.solapi.sdk.message.dto.response.MultipleDetailMessageSentResponse;

import java.util.Set;

public interface SmsService {
    MultipleDetailMessageSentResponse sendTerminationNotice(String rawTo, Set<String> abuseTypes);
}