package callprotector.spring.service.ChatbotService;

import callprotector.spring.domain.ChatLog;
import callprotector.spring.domain.ChatSession;
import callprotector.spring.repository.ChatLogRepository;
import callprotector.spring.repository.ChatSessionRepository;
import callprotector.spring.web.dto.response.ChatLogResponseDTO;
import callprotector.spring.web.dto.response.ChatbotResponseDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatLogServiceImpl implements ChatLogService {

    private final ChatLogRepository chatLogRepository;
    private final ChatSessionRepository chatSessionRepository;

    private final RestTemplate restTemplate = new RestTemplate(); // FastAPI 호출용

    private final String chatbotUrl = "http://localhost:8000/ask"; // FASTAPI 호출 ID


    // SSE 용 saveChatLog (ChatStreamController가 호출함)
    @Override
    @Transactional
    public void saveChatLog(Long sessionId, String question, String answer) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));

        chatLogRepository.save(ChatLog.builder()
                .chatSession(session)
                .question(question)
                .answer(answer)
                .build());

    }


    @Override
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



    // SSE로 갈아타면서 이건 안 쓰게 됨
    @Override
    public ChatLogResponseDTO.ChatLogResponse askAndSaveChatLog(Long sessionId, String question) {
        // 1. 세션 조회
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 세션 ID"));

        // FastAPI 요청 로그
        log.info("📤 FastAPI 요청: sessionId={}, question={}", sessionId, question);

        // 2. FastAPI 챗봇 호출
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = Map.of(
                "session_id", sessionId,
                "question", question
        );

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

        ChatbotResponseDTO.ChatbotResponse chatbotResponse = restTemplate
                .exchange(chatbotUrl, HttpMethod.POST, requestEntity, ChatbotResponseDTO.ChatbotResponse.class)
                .getBody();

        // FastAPI 응답 로그
        if (chatbotResponse != null) {
            log.info("📥 FastAPI 응답: answer={}, sources={}",
                    chatbotResponse.getAnswer(), chatbotResponse.getSourcePages());
        } else {
            log.warn("⚠️ FastAPI 응답이 null입니다.");
        }

        // 3. ChatLog 저장
        ChatLog chatLog = ChatLog.of(session, question, chatbotResponse.getAnswer(), chatbotResponse.getSourcePages());
        chatLogRepository.save(chatLog);

        log.info("💾 ChatLog 저장 완료: id={}, createdAt={}", chatLog.getId(), chatLog.getCreatedAt());

        // 4. DTO 변환 후 반환
        return ChatLogResponseDTO.ChatLogResponse.builder()
                .id(chatLog.getId())
                .question(chatLog.getQuestion())
                .answer(chatLog.getAnswer())
                .createdAt(chatLog.getCreatedAt().toString())
                .build();

    }
}
