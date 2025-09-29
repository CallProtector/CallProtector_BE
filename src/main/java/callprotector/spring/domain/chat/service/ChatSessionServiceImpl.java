package callprotector.spring.domain.chat.service;

import callprotector.spring.domain.chat.entity.ChatSession;
import callprotector.spring.domain.user.entity.User;
import callprotector.spring.domain.chat.repository.ChatSessionRepository;
import callprotector.spring.domain.chat.dto.response.ChatSessionResponseDTO;
import callprotector.spring.global.ai.OpenAiService.OpenAiTitleService;
import callprotector.spring.global.apiPayload.code.status.ErrorStatus;
import callprotector.spring.global.apiPayload.exception.handler.ChatGeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .orElseThrow(() -> new ChatGeneralException(ErrorStatus.CHAT_SESSION_NOT_FOUND));
    }

    @Override
    @Transactional
    public ChatSessionResponseDTO.ChatSessionResponse createSession(User user) {
        log.info("JWT 인증된 사용자 이메일: {}", user.getId());

        ChatSession session = ChatSession.builder()
                .user(user)
                .status(1) // 진행 중
                .build();

        ChatSession saved = chatSessionRepository.save(session);

        return ChatSessionResponseDTO.ChatSessionResponse.builder()
                .sessionId(saved.getId())
                .startTime(saved.getCreatedAt().toString())
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
        return chatSessionRepository.findAllForUserOrderByLastUserQuestion(userId).stream()
                .map(session -> ChatSessionResponseDTO.ChatSessionResponse.builder()
                        .sessionId(session.getId())
                        .title(session.getTitle())
                        .startTime(session.getCreatedAt().toString())
                        .lastUserQuestionAt(session.getLastUserQuestionAt() != null
                                ? session.getLastUserQuestionAt().toString()
                                : null)
                        .build())
                .toList();
    }

}
