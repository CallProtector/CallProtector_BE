package callprotector.spring.service.ChatSessionService;

import callprotector.spring.domain.ChatSession;
import callprotector.spring.domain.User;
import callprotector.spring.repository.ChatSessionRepository;
import callprotector.spring.repository.UserRepository;
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
    private final UserRepository userRepository;

    @Override
    public ChatSessionResponseDTO.ChatSessionResponse createSession(String email) {
        log.info("JWT 인증된 사용자 이메일: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자"));

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
