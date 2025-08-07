package callprotector.spring.domain.chat.service;

import callprotector.spring.domain.chat.dto.request.ScriptHistoryRequestDTO;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ChatbotService {

    public Flux<ServerSentEvent<String>> analyzeCallsession(Long sessionId, List<ScriptHistoryRequestDTO.ScriptHistoryDTO> scriptHistory);
}
