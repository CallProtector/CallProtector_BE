package callprotector.spring.domain.chat.service;

import callprotector.spring.domain.callsession.entity.CallSession;
import callprotector.spring.domain.callchat.dto.response.CallChatSessionResponseDTO;
import callprotector.spring.domain.callchat.entity.CallChatSession;
import callprotector.spring.domain.chat.repository.CallChatSessionRepository;
import callprotector.spring.domain.user.entity.User;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallChatSessionServiceImpl implements CallChatSessionService{

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
    @Transactional
    public CallChatSessionResponseDTO.CallChatSessionResponse createCallChatSession(User user, CallSession callSession) {
        String callsessionCode = callSession.getCallSessionCode(); // title callsessionCode 사용

        CallChatSession session = CallChatSession.builder()
                .user(user)
                .callSession(callSession)
                .title(callsessionCode)
                .build();

        CallChatSession saved = callChatSessionRepository.save(session);

        return CallChatSessionResponseDTO.CallChatSessionResponse.builder()
                .sessionId(saved.getId())
                .title(saved.getTitle())
                .createdAt(saved.getCreatedAt().toString())
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public CallChatSession getSessionById(Long sessionId) {
        return callChatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("상담 기반 세션을 찾을 수 없습니다."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CallChatSessionResponseDTO.CallChatSessionResponse> getSessionListDtoByUserId(Long userId) {
        return callChatSessionRepository.findByUserIdOrderByIdDesc(userId).stream()
                .map(session -> CallChatSessionResponseDTO.CallChatSessionResponse.builder()
                        .sessionId(session.getId())
                        .createdAt(session.getCreatedAt().toString()) // BaseEntity.getCreatedAt()
                        .title(session.getTitle())
                        .build())
                .toList();
    }



}
