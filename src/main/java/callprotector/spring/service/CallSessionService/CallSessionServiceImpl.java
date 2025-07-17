package callprotector.spring.service.CallSessionService;

import callprotector.spring.config.SttWebSocketHandler;
import callprotector.spring.domain.CallSession;
import callprotector.spring.domain.User;
import callprotector.spring.repository.CallSessionRepository;
import callprotector.spring.repository.UserRepository;
import callprotector.spring.service.util.CallSessionCodeGenerator;
import callprotector.spring.web.dto.request.CallSessionRequestDTO;
import callprotector.spring.web.dto.response.CallSessionResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.NoSuchElementException;

import com.twilio.rest.api.v2010.account.Call;
import com.twilio.exception.ApiException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallSessionServiceImpl implements CallSessionService {

    private final CallSessionRepository callSessionRepository;
    private final UserRepository userRepository;
    private final CallSessionCodeGenerator codeGenerator;
    private final SttWebSocketHandler sttWebSocketHandler;

    @Override
    @Transactional
    public Long createCallSession(String email, CallSessionRequestDTO.CallSessionMakeDTO dto) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다. ID=" + dto.getUserId()));

        // 세션 코드 생성
        String sessionCode = codeGenerator.generateTodayCallSessionCode();

        CallSession session = CallSession.builder()
                .callSessionCode(sessionCode) // 추가
                .user(user)
                .title(dto.getTitle())
                .twilioCallSid(dto.getTwilioCallSid())
                .build();
        callSessionRepository.save(session);
        return session.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public CallSessionResponseDTO.CallSessionInfoDTO getCallSessionInfo(final Long callSessionId) {
        CallSession callSession = callSessionRepository.findById(callSessionId)
                .orElseThrow(() -> new IllegalArgumentException("해당 CallSession이 존재하지 않습니다. ID: " + callSessionId));

        String formattedCreatedAt = formatCreatedAt(callSession.getCreatedAt());

        return CallSessionResponseDTO.CallSessionInfoDTO.builder()
                .callSessionCode(callSession.getCallSessionCode())
                .createdAt(formattedCreatedAt)
                .totalAbuseCnt(callSession.getTotalAbuseCnt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CallSession getCallSession(Long callSessionId) {
        return callSessionRepository.findById(callSessionId)
            .orElseThrow(() -> new NoSuchElementException("CallSession not found with ID: " + callSessionId));
    }

    @Override
    @Transactional
    public void incrementTotalAbuseCnt(Long callSessionId) {
        callSessionRepository.findById(callSessionId)
            .ifPresentOrElse(session -> {
                int currentAbuseCnt = session.getTotalAbuseCnt();
                session.updateAbuseCnt();

                // totalAbuseCnt가 0에서 1로 변경될 때 abuseTag를 true로 설정
                if (currentAbuseCnt == 0 && session.getTotalAbuseCnt() == 1) {
                    session.updateAbuseTag();
                    log.info("CallSession (ID: {})의 abuse_tag {}", session.getId(), session.getAbuseTag());
                }


                log.info("CallSession (ID: {})의 total_abuse_cnt가 {}로 업데이트 성공",
                    session.getId(), session.getTotalAbuseCnt());

                if (session.getUser() != null) {
                    CallSessionResponseDTO.CallSessionTotalAbuseCntDTO updateDto = CallSessionResponseDTO.CallSessionTotalAbuseCntDTO.builder()
                        .sessionId(session.getId())
                        .totalAbuseCnt(session.getTotalAbuseCnt())
                        .build();

                    sttWebSocketHandler.sendUpdateAbuseCntToClient(
                        session.getUser().getId(),
                        updateDto
                    );
                    log.info("WebSocket으로 totalAbuseCnt 업데이트 메시지 전송 완료 ===== callSessionId={}, totalAbuseCnt={}",
                        session.getId(), session.getTotalAbuseCnt());
                } else {
                    log.warn("CallSession에 연결된 User가 없어 totalAbuseCnt 업데이트 메시지 전송 실패 ===== CallSessionId={}", session.getId());
                }

                // 3회 이상 폭언 시 강제 종료
                if (session.getTotalAbuseCnt() >= 3) {
                    forceTerminateCall(session);
                }

            }, () -> {
                log.warn("CallSession (ID={})을(를) 찾을 수 없습니다. STT 로그의 total_abuse_cnt 업데이트 실패", callSessionId);
                // CallSession을 찾지 못했을 때의 추가적인 에러 처리 로직
            });
    }

    @Override
    @Transactional
    public void forceTerminateCall(CallSession callSession) {
        log.warn("🚨 CallSession (ID: {})의 total_abuse_cnt 초과로 통화를 강제 종료 요청합니다.",
            callSession.getId()); // CallSession 객체에서 ID를 직접 가져옴

        String callSid = callSession.getTwilioCallSid();

        if (callSid != null && !callSid.isEmpty()) {
            try {
                // 통화 상태를 'completed'로 변경하여 종료
                Call.updater(callSid).setStatus(Call.UpdateStatus.COMPLETED).update();
                log.info("Twilio Call SID {} - 통화 종료 성공.", callSid);

                callSession.updateEndedAt();
            } catch (ApiException e) {
                log.error("❌ Twilio 통화 종료 실패 (Call SID: {}): {}", callSid, e.getMessage(), e);
            }
        } else {
            log.warn("❗ CallSession (ID: {})에 Twilio Call SID가 없어 강제 통화 종료 실패", callSession.getId());
        }
    }

    private String formatCreatedAt(LocalDateTime createdAt) {
        String datePart = createdAt.format(DateTimeFormatter.ofPattern("M.d", Locale.KOREA));
        String timePart = createdAt.format(DateTimeFormatter.ofPattern("HH:mm", Locale.KOREA));
        String dayKor = getKoreanDayOfWeek(createdAt.getDayOfWeek());

        return String.format("%s (%s) %s", datePart, dayKor, timePart);
    }

    private String getKoreanDayOfWeek(DayOfWeek dayOfWeek) {
		return switch (dayOfWeek) {
			case MONDAY -> "월";
			case TUESDAY -> "화";
			case WEDNESDAY -> "수";
			case THURSDAY -> "목";
			case FRIDAY -> "금";
			case SATURDAY -> "토";
			case SUNDAY -> "일";
		};
    }


}
