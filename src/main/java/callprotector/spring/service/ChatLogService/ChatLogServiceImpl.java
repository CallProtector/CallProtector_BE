package callprotector.spring.service.ChatLogService;

import callprotector.spring.domain.ChatLog;
import callprotector.spring.domain.ChatSession;
import callprotector.spring.repository.ChatLogRepository;
import callprotector.spring.service.ChatSessionService.ChatSessionService;
import callprotector.spring.web.dto.response.ChatLogResponseDTO;
import callprotector.spring.web.dto.response.ChatbotResponseDTO;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatLogServiceImpl implements ChatLogService {

    private final ChatLogRepository chatLogRepository;
    private final ChatSessionService chatSessionService; // 리포지터리 대신 서비스에 의존하기

    private final RestTemplate restTemplate = new RestTemplate(); // FastAPI 호출용

    private final String chatbotUrl = "http://localhost:8000/ask"; // FASTAPI 호출 ID


    // SSE 용 saveChatLog (ChatStreamController가 호출함)
    @Override
    @Transactional
    public void saveChatLog(Long sessionId, String question, String answer) {
        ChatSession session = chatSessionService.getSessionById(sessionId);

        chatLogRepository.save(ChatLog.builder()
                .chatSession(session)
                .question(question)
                .answer(answer)
                .build());

    }


    @Override
    @Transactional(readOnly = true)
    public List<ChatLogResponseDTO.ChatLogResponse> getChatLogsBySession(Long sessionId) {
        return chatLogRepository.findAllByChatSessionId(sessionId).stream()
                .map(log -> ChatLogResponseDTO.ChatLogResponse.builder()
                        .id(log.getId())
                        .question(log.getQuestion())
                        .answer(log.getAnswer())
                        .createdAt(log.getCreatedAt().toString())
                        .build())
                .toList();
    }



}
