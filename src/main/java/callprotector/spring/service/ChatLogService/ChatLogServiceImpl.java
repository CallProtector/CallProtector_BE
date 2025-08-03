package callprotector.spring.service.ChatLogService;

import callprotector.spring.domain.ChatLog;
import callprotector.spring.domain.ChatSession;
import callprotector.spring.repository.ChatLogRepository;
import callprotector.spring.service.ChatSessionService.ChatSessionService;
import callprotector.spring.web.dto.response.ChatLogResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatLogServiceImpl implements ChatLogService {

    private final ChatLogRepository chatLogRepository;
    private final ChatSessionService chatSessionService; // 리포지터리 대신 서비스에 의존하기


    // SSE 용 saveChatLog (ChatStreamController가 호출함)
    @Override
    @Transactional
    public void saveChatLog(Long sessionId, String question, String answer, String sourcePages) {
        ChatSession session = chatSessionService.getSessionById(sessionId);

        chatLogRepository.save(ChatLog.builder()
                .chatSession(session)
                .question(question)
                .answer(answer)
                .sourcePages(sourcePages) // JSON 문자열 그대로 저장
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
                        .sourcePages(log.getSourcePages()) // 추가
                        .createdAt(log.getCreatedAt().toString())
                        .build())
                .toList();
    }



}
