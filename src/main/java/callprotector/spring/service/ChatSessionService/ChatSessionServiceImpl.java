package callprotector.spring.service.ChatSessionService;

import callprotector.spring.domain.ChatSession;
import callprotector.spring.domain.User;
import callprotector.spring.repository.ChatSessionRepository;
import callprotector.spring.web.dto.response.ChatSessionResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService{

    private final ChatSessionRepository chatSessionRepository;


    @Override
    public ChatSession getSessionById(Long sessionId) {
        return chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));
    }

    @Override
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
