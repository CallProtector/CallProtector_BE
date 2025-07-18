package callprotector.spring.service.CallSessionService;

import callprotector.spring.apiPayload.exception.handler.CallSessionNotFoundException;
import callprotector.spring.config.SttWebSocketHandler;
import callprotector.spring.domain.CallSession;
import callprotector.spring.domain.CallSttLog;
import callprotector.spring.domain.User;
import callprotector.spring.repository.CallSessionRepository;
import callprotector.spring.repository.UserRepository;
import callprotector.spring.service.CallSttLogService.CallSttLogService;
import callprotector.spring.service.util.CallSessionCodeGenerator;
import callprotector.spring.web.dto.request.CallSessionRequestDTO;
import callprotector.spring.web.dto.response.CallSessionResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

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
    private final CallSttLogService callSttLogService;

    @Override
    @Transactional
    public Long createCallSession(String email, CallSessionRequestDTO.CallSessionMakeDTO dto) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다. ID=" + dto.getUserId()));

        // 세션 코드 생성
        String sessionCode = codeGenerator.generateTodayCallSessionCode();

        String rawNumber = dto.getCallerNumber();
        String formattedNumber = formatKoreanPhoneNumber(rawNumber);

        CallSession session = CallSession.builder()
                .callSessionCode(sessionCode)
                .user(user)
                .twilioCallSid(dto.getTwilioCallSid())
                .callerNumber(formattedNumber)
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

    @Override
    @Transactional(readOnly = true)
    public CallSessionResponseDTO.CallSessionDetailResponseDTO getCallSessionDetail(final Long callSessionId) {
        // sessionInfo
        CallSession callSession = findCallSessionById(callSessionId);
        CallSessionResponseDTO.CallSessionInfoDTO sessionInfoDTO = mapToSessionInfoDTO(callSession);

        // scriptHistory
        List<CallSttLog> scriptLogs = callSttLogService.getAllBySessionId(callSessionId);
        List<CallSessionResponseDTO.CallSessionScriptDTO> sessionScriptDTO = mapToScriptDTO(scriptLogs);

        // TODO: aiSummary 추가
        return CallSessionResponseDTO.CallSessionDetailResponseDTO.builder()
            .sessionInfo(sessionInfoDTO)
            .scriptHistory(sessionScriptDTO)
            .build();
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

    // 발신번호 포맷팅 함수
    private String formatKoreanPhoneNumber(String rawNumber) {
        if (rawNumber == null || rawNumber.isBlank()) return null;

        // +82로 시작하는 국제번호 처리
        if (rawNumber.startsWith("+82")) {
            String local = rawNumber.substring(3);
            if (local.startsWith("10") && local.length() == 10) {
                return "010-" + local.substring(2, 6) + "-" + local.substring(6);
            }
        }

        return rawNumber;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CallSessionResponseDTO.CallSessionListDTO> getCallSessions(String sortBy, String order) {
        Sort.Direction direction = order.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

        List<CallSession> sessions = callSessionRepository.findAll(Sort.by(direction, sortBy));

        return sessions.stream()
                .map(CallSessionResponseDTO.CallSessionListDTO::fromEntity)
                .collect(Collectors.toList());
    }

    private CallSession findCallSessionById(final Long callSessionId) {
        return callSessionRepository.findById(callSessionId).orElseThrow(CallSessionNotFoundException::new);
    }

    private CallSessionResponseDTO.CallSessionInfoDTO mapToSessionInfoDTO(CallSession callSession) {
        return CallSessionResponseDTO.CallSessionInfoDTO.builder()
            .callSessionCode(callSession.getCallSessionCode())
            .createdAt(formatCreatedAt(callSession.getCreatedAt()))
            .totalAbuseCnt(callSession.getTotalAbuseCnt())
            .build();
    }


    private List<CallSessionResponseDTO.CallSessionScriptDTO> mapToScriptDTO(List<CallSttLog> scriptLogs) {

        return scriptLogs.stream()
            .map(log -> {
                return CallSessionResponseDTO.CallSessionScriptDTO.builder()
                    .id(log.getId())
                    .callSessionId(log.getCallSessionId())
                    .speaker(log.getTrack().toString())
                    .text(log.getScript())
                    .isAbuse(log.getIsAbuse())
                    .abuseType(log.getAbuseType())
                    .timestamp(log.getTimestamp())
                    .build();
            })
            .collect(Collectors.toList());
    }
}
