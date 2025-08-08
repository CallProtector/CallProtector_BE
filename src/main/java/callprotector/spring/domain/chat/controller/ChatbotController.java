package callprotector.spring.domain.chat.controller;

import callprotector.spring.domain.chat.dto.request.SessionScriptHistoryRequestDTO;
import callprotector.spring.domain.chat.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping(value = "/analyze/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> analyzeSession(@PathVariable Long sessionId, @RequestBody List<SessionScriptHistoryRequestDTO.ScriptHistoryDTO> scripts){
        return chatbotService.analyzeCallsession(sessionId, scripts);
    }


}
