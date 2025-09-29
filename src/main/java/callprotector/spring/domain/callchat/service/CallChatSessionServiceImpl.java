package callprotector.spring.domain.callchat.service;

import callprotector.spring.domain.callsession.entity.CallSession;
import callprotector.spring.domain.callchat.dto.response.CallChatSessionResponseDTO;
import callprotector.spring.domain.callchat.entity.CallChatSession;
import callprotector.spring.domain.callchat.repository.CallChatSessionRepository;
import callprotector.spring.domain.user.entity.User;
import callprotector.spring.global.apiPayload.code.status.ErrorStatus;
import callprotector.spring.global.apiPayload.exception.handler.CallChatGeneralException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallChatSessionServiceImpl implements CallChatSessionService {

    private final CallChatSessionRepository callChatSessionRepository;

    @Override
    @Transactional
    public CallChatSession getOrCreate(User user, CallSession callSession) {
        return callChatSessionRepository
                .findByUserIdAndCallSessionId(user.getId(), callSession.getId())
                .orElseGet(() -> {
                    CallChatSession saved = callChatSessionRepository.save(
                            CallChatSession.builder()
                                    .user(user)
                                    .callSession(callSession)
                                    .title(callSession.getCallSessionCode())
                                    .build()
                    );
                    return saved;
                });
    }


    @Override
    @Transactional(readOnly = true)
    public CallChatSession getSessionById(Long sessionId) {
        return callChatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new CallChatGeneralException(ErrorStatus.CALLCHAT_SESSION_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CallChatSessionResponseDTO.CallChatSessionResponse> getSessionListDtoByUserId(Long userId) {
        return callChatSessionRepository.findAllForUserOrderByLastUserQuestion(userId).stream()
                .map(session -> CallChatSessionResponseDTO.CallChatSessionResponse.builder()
                        .sessionId(session.getId())
                        .createdAt(session.getCreatedAt().toString()) // BaseEntity.getCreatedAt()
                        .title(session.getTitle())
                        .lastUserQuestionAt(session.getLastUserQuestionAt() != null
                                ? session.getLastUserQuestionAt().toString()
                                : null)
                        .build())
                .toList();
    }



}
