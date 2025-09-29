package callprotector.spring.domain.chat.service;

import callprotector.spring.domain.chat.dto.response.SourcePageDTO;
import callprotector.spring.domain.chat.entity.ChatLog;
import callprotector.spring.domain.chat.entity.ChatSession;
import callprotector.spring.domain.chat.repository.ChatLogRepository;
import callprotector.spring.domain.chat.dto.response.ChatLogResponseDTO;
import callprotector.spring.global.apiPayload.code.status.ErrorStatus;
import callprotector.spring.global.apiPayload.exception.handler.ChatGeneralException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatLogServiceImpl implements ChatLogService {

    private final ChatLogRepository chatLogRepository;
    private final ChatSessionService chatSessionService; // 리포지터리 대신 서비스에 의존하기
    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON 파싱용


    // SSE 용 saveChatLog (ChatStreamController가 호출함)
    @Override
    @Transactional
    public void saveChatLog(Long sessionId, String question, String answer, String sourcePages) {
        try {
            ChatSession session = chatSessionService.getSessionById(sessionId);

            ChatLog saved = chatLogRepository.save(
                    ChatLog.builder()
                            .chatSession(session)
                            .question(question)
                            .answer(answer)
                            .sourcePages(sourcePages)
                            .build()
            );
            // 사용자 질문이 존재하면 마지막 사용자 질문 시각 갱신
            if (question != null && !question.isBlank()) {
                session.touchLastUserQuestionAt(saved.getCreatedAt());
            }
        } catch (Exception e) {
            log.error("❌ 채팅 로그 저장 실패", e);
            throw new ChatGeneralException(ErrorStatus.CHAT_LOG_SAVE_FAILED);
        }

    }


    @Override
    @Transactional(readOnly = true)
    public List<ChatLogResponseDTO.ChatLogResponse> getChatLogsBySession(Long sessionId) {
        return chatLogRepository.findAllByChatSessionId(sessionId).stream()
                .map(log -> {
                    List<SourcePageDTO.SourcePage> sourcePagesList;
                    try {
                        sourcePagesList = objectMapper.readValue(
                                log.getSourcePages(),
                                new TypeReference<>() {} // Java 11이상에서는 타입 추론 가능
                        );
                    } catch (Exception e) {
                        sourcePagesList = List.of(); // 파싱 실패 시 빈 리스트 반환 (타입 추론 자동 적용)
                    }

                    return ChatLogResponseDTO.ChatLogResponse.builder()
                            .id(log.getId())
                            .question(log.getQuestion())
                            .answer(log.getAnswer())
                            .sourcePages(sourcePagesList) // 리스트로 반환
                            .createdAt(log.getCreatedAt().toString())
                            .build();
                })
                .toList();
    }



}
