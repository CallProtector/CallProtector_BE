package callprotector.spring.domain.callchat.service;

import callprotector.spring.domain.callchat.dto.response.CallChatLogResponseDTO;
import callprotector.spring.domain.chat.dto.response.SourcePageDTO;
import callprotector.spring.domain.callchat.entity.CallChatLog;
import callprotector.spring.domain.callchat.entity.CallChatSession;
import callprotector.spring.domain.callchat.repository.CallChatLogRepository;
import callprotector.spring.global.apiPayload.code.status.ErrorStatus;
import callprotector.spring.global.apiPayload.exception.handler.CallChatGeneralException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallChatLogServiceImpl implements CallChatLogService {

    private final CallChatLogRepository callChatLogRepository;
    private final CallChatSessionService callChatSessionService;

    @Override
    @Transactional
    public void saveCallChatLog(Long sessionId, String question, String answer, String sourcePages) {
        try {
            CallChatSession session = callChatSessionService.getSessionById(sessionId);

            CallChatLog saved = callChatLogRepository.save(
                    CallChatLog.builder()
                            .callChatSession(session)
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
            log.error("❌ 상담별 채팅 로그 저장 실패", e);
            throw new CallChatGeneralException(ErrorStatus.CALLCHAT_LOG_SAVE_FAILED);
        }

    }

    @Override
    @Transactional(readOnly = true)
    public List<CallChatLogResponseDTO.CallChatLogResponse> getLogDtosBySession(Long sessionId) {
        return callChatLogRepository.findBySessionIdWithOrder(sessionId).stream()
                .map(log -> {
                    List<SourcePageDTO.SourcePage> sourcePagesList;
                    try {
                        sourcePagesList = new ObjectMapper().readValue(
                                log.getSourcePages(),
                                new com.fasterxml.jackson.core.type.TypeReference<>() {}
                        );
                    } catch (Exception e) {
                        sourcePagesList = List.of(); // 실패 시 빈 리스트
                    }

                    return CallChatLogResponseDTO.CallChatLogResponse.builder()
                            .id(log.getId())
                            .question(log.getQuestion())
                            .answer(log.getAnswer())
                            .sourcePages(sourcePagesList)
                            .createdAt(log.getCreatedAt().toString())
                            .build();
                })
                .toList();
    }

}
