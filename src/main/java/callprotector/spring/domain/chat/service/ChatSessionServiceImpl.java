package callprotector.spring.domain.chat.service;

import callprotector.spring.domain.chat.entity.ChatSession;
import callprotector.spring.domain.user.entity.User;
import callprotector.spring.domain.chat.repository.ChatSessionRepository;
import callprotector.spring.domain.chat.dto.response.ChatSessionResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService{

    private final ChatSessionRepository chatSessionRepository;


    @Override
    @Transactional(readOnly = true)
    public ChatSession getSessionById(Long sessionId) {
        return chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));
    }

    @Override
    @Transactional
    public ChatSessionResponseDTO.ChatSessionResponse createSession(User user) {
        log.info("JWT 인증된 사용자 이메일: {}", user.getId());

        ChatSession session = ChatSession.builder()
                .user(user)
                .startTime(LocalDateTime.now())
                .status(1) // 진행 중
                .build();

        ChatSession saved = chatSessionRepository.save(session);

        return ChatSessionResponseDTO.ChatSessionResponse.builder()
                .sessionId(saved.getId())
                .startTime(saved.getStartTime().toString())
                .build();
    }

}
