package callprotector.spring.domain.chat.service;

import callprotector.spring.domain.callchat.dto.response.CallChatLogResponseDTO;
import callprotector.spring.domain.chat.dto.response.SourcePageDTO;
import callprotector.spring.domain.callchat.entity.CallChatLog;
import callprotector.spring.domain.callchat.entity.CallChatSession;
import callprotector.spring.domain.callchat.repository.CallChatLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallChatLogServiceImpl implements CallChatLogService{

    private final CallChatLogRepository callChatLogRepository;

    private final CallChatSessionService callChatSessionService;

    @Override
    @Transactional
    public void saveCallChatLog(Long sessionId, String question, String answer, String sourcePages) {
        CallChatSession session = callChatSessionService.getSessionById(sessionId);

        CallChatLog log = CallChatLog.builder()
                .callChatSession(session)
                .question(question)
                .answer(answer)
                .sourcePages(sourcePages)
                .build();

        callChatLogRepository.save(log);

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
