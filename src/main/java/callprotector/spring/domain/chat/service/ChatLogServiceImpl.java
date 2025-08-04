package callprotector.spring.domain.chat.service;

import callprotector.spring.domain.chat.entity.ChatLog;
import callprotector.spring.domain.chat.entity.ChatSession;
import callprotector.spring.domain.chat.repository.ChatLogRepository;
import callprotector.spring.domain.chat.dto.response.ChatLogResponseDTO;
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
