package callprotector.spring.domain.chat.service;

import callprotector.spring.domain.chat.entity.ChatSession;
import callprotector.spring.domain.user.entity.User;
import callprotector.spring.domain.chat.repository.ChatSessionRepository;
import callprotector.spring.domain.chat.dto.response.ChatSessionResponseDTO;
import callprotector.spring.global.ai.OpenAiService.OpenAiTitleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService{

    private final ChatSessionRepository chatSessionRepository;
    private final OpenAiTitleService openAiTitleService;


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
                .title(saved.getTitle()) // title 추가
                .build();
    }

    @Override
    @Transactional
    public String updateTitleIfEmpty(ChatSession session, String firstQuestion) {
        if (session.getTitle() == null || session.getTitle().isBlank()) {
            String generatedTitle = openAiTitleService.generateTitle(firstQuestion);
            session.setTitle(generatedTitle);
            chatSessionRepository.save(session);
        }
        return session.getTitle();

    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatSessionResponseDTO.ChatSessionResponse> getSessionList(Long userId) {
        return chatSessionRepository.findByUserIdOrderByStartTimeDesc(userId).stream()
                .map(session -> ChatSessionResponseDTO.ChatSessionResponse.builder()
                        .sessionId(session.getId())
                        .title(session.getTitle())
                        .startTime(session.getStartTime().toString())
                        .build())
                .toList();
    }

    // getSessionDetail 함수 하려는데, ScriptHistoryRepository 뭐꼬 이거;; (MongoRepository로 callSessionId 기준 스크립트 이력 조회해야된다는데)





}
