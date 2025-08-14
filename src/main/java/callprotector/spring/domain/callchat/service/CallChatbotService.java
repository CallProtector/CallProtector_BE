package callprotector.spring.domain.callchat.service;

import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

public interface CallChatbotService {

    public Flux<ServerSentEvent<String>> analyzeCallsession(Long sessionId, Long userId);
}
