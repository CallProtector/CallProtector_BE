package callprotector.spring.service.CallSessionService;

import callprotector.spring.domain.CallSession;
import callprotector.spring.domain.User;
import callprotector.spring.repository.CallSessionRepository;
import callprotector.spring.repository.UserRepository;
import callprotector.spring.service.util.CallSessionCodeGenerator;
import callprotector.spring.web.dto.request.CallSessionRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CallSessionServiceImpl implements CallSessionService {

    private final CallSessionRepository callSessionRepository;
    private final UserRepository userRepository;
    private final CallSessionCodeGenerator codeGenerator;

    @Override
    public Long createCallSession(String email, CallSessionRequestDTO.CallSessionMakeDTO dto) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다. ID=" + dto.getUserId()));


        // 세션 코드 생성
        String sessionCode = codeGenerator.generateTodayCallSessionCode();

        CallSession session = CallSession.builder()
                .callSessionCode(sessionCode) // 추가
                .user(user)
                .title(dto.getTitle())
                .abuseTag(false) // 초기 상태: false 또는 null
                .build();
        callSessionRepository.save(session);
        return session.getId();
    }
}
