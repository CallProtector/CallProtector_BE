package callprotector.spring.service.CallSessionService;

import callprotector.spring.apiPayload.code.status.ErrorStatus;
import callprotector.spring.apiPayload.exception.handler.*;
import callprotector.spring.domain.enums.CallTrack;
import callprotector.spring.handler.SttWebSocketHandler;
import callprotector.spring.domain.*;
import callprotector.spring.domain.mapping.AbuseTypeLog;
import callprotector.spring.repository.*;
import callprotector.spring.service.CallSttLogService.CallSttLogService;
import callprotector.spring.service.GeminiService.GeminiService;
import callprotector.spring.service.OpenAiService.OpenAiSummaryService;
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
import java.util.*;
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
    private final AbuseLogRepository abuseLogRepository;
    private final AbuseTypeLogRepository abuseTypeLogRepository;
    private final CallLogRepository callLogRepository;
    private final CallSttLogSearchRepository callSttLogSearchRepository;
    private final OpenAiSummaryService openAiSummaryService;
    private final GeminiService geminiService;

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
        CallSession callSession = findCallSessionById(callSessionId);

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
    public CallSessionResponseDTO.CallSessionDetailResponseDTO getUserCallSessionDetail(final Long callSessionId, final Long userId) {
        // sessionInfo
        CallSession callSession = findCallSessionByIdAndUserId(callSessionId, userId);
        CallSessionResponseDTO.CallSessionInfoDTO sessionInfoDTO = mapToSessionInfoDTO(callSession);

        // scriptHistory
        List<CallSttLog> scriptLogs = callSttLogService.getAllBySessionId(callSessionId);
        List<CallSessionResponseDTO.CallSessionScriptDTO> sessionScriptDTO = mapToScriptDTO(scriptLogs);

        // aiSummary - Gemini
        // TODO : AI 요약 구현 방식에 따라 변경 필요
        String aiSummary = callSession.getSummaryGemini();
        return CallSessionResponseDTO.CallSessionDetailResponseDTO.builder()
            .sessionInfo(sessionInfoDTO)
            .scriptHistory(sessionScriptDTO)
            .aiSummary(aiSummary)
            .build();
    }



    @Override
    @Transactional(readOnly = true)
    public CallSessionResponseDTO.CallSessionPagingDTO getCallSessions(String sortBy, String order, Long cursorId, int size) {
        Sort.Direction direction = order.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

        List<CallSession> sessions = (cursorId == null)
                ? callSessionRepository.findFirstPage(sortBy, size + 1, direction)
                : callSessionRepository.findByCursor(sortBy, cursorId, size + 1, direction);

        boolean hasNext = sessions.size() > size;
        Long nextCursorId = hasNext ? sessions.get(size - 1).getId() : null;

        List<CallSessionResponseDTO.CallSessionListDTO> resultList = sessions.stream()
                .limit(size)
                .map(session -> {
                    String category = getAbuseCategoryForSession(session);
                    return CallSessionResponseDTO.CallSessionListDTO.fromEntity(session, category);
                })
                .collect(Collectors.toList());

        return CallSessionResponseDTO.CallSessionPagingDTO.builder()
                .sessions(resultList)
                .hasNext(hasNext)
                .nextCursorId(nextCursorId)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CallSessionResponseDTO.CallSessionPagingDTO getSessionsByAbuseCategory(String category, Long cursorId, int size, String order) {
        validateAbuseCategory(category);

        Sort.Direction direction = order.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

        List<CallSession> sessions = callSessionRepository.findSessionsByAbuseCategory(category, cursorId, size + 1, direction);

        boolean hasNext = sessions.size() > size;
        Long nextCursorId = hasNext ? sessions.get(size - 1).getId() : null;

        List<CallSessionResponseDTO.CallSessionListDTO> resultList = sessions.stream()
                .limit(size)
                .map(session -> {
                    String resolvedCategory = getAbuseCategoryForSession(session);
                    return CallSessionResponseDTO.CallSessionListDTO.fromEntity(session, resolvedCategory);
                })
                .collect(Collectors.toList());

        return CallSessionResponseDTO.CallSessionPagingDTO.builder()
                .sessions(resultList)
                .hasNext(hasNext)
                .nextCursorId(nextCursorId)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CallSessionResponseDTO.CallSessionPagingDTO searchCallSessions(String keyword, String category, String order, Long cursorId, int size) {
        // Elasticsearch에서 CallSttLog 검색
        List<CallSttLog> sttLogs = callSttLogSearchRepository.searchByKeywordAndFilters(keyword, category, order, cursorId, size + 1);

        // callSessionId → script 매핑
        Map<Long, String> sessionScriptMap = sttLogs.stream()
                .collect(Collectors.toMap(
                        CallSttLog::getCallSessionId,
                        CallSttLog::getScript,
                        (existing, replacement) -> existing // 중복 키 발생 시 첫 번째 script 유지
                ));

        // CallSession ID 추출
        List<Long> sessionIds = new ArrayList<>(sessionScriptMap.keySet()).stream()
                .limit(size + 1)
                .toList();

        // 세션 정렬 후 조회
        Sort.Direction direction = order.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        List<CallSession> sessions = callSessionRepository.findByIdsWithOrder(sessionIds, direction);

        // DTO 변환
        boolean hasNext = sessionIds.size() > size;
        Long nextCursorId = hasNext ? sessionIds.get(size - 1) : null;

        List<CallSessionResponseDTO.CallSessionListDTO> resultList = sessions.stream()
                .limit(size)
                .map(session -> {
                    String resolvedCategory = getAbuseCategoryForSession(session);
                    String fullScript = sessionScriptMap.get(session.getId());
                    String matchedScript = extractSnippetAroundKeyword(fullScript, keyword, 15);
                    return CallSessionResponseDTO.CallSessionListDTO.fromEntityWithScript(session, resolvedCategory, matchedScript);
                })
                .toList();

        log.info("📌 keyword: {}, category: {}, order: {}, cursorId: {}, size: {}", keyword, category, order, cursorId, size); //추가

        return CallSessionResponseDTO.CallSessionPagingDTO.builder()
                .sessions(resultList)
                .hasNext(hasNext)
                .nextCursorId(nextCursorId)
                .build();
    }


    @Override
    public String generateSummaryByOpenAi(Long callSessionId) {
        CallSession session = findCallSessionById(callSessionId);

        if (session.getSummary() != null && !session.getSummary().isBlank()) {
            log.info("✅ 기존 요약 반환 - CallSession ID: {}", callSessionId);
            return session.getSummary();
        }

        try {
            List<CallSttLog> sttLogs = callSttLogService.getAllBySessionId(callSessionId);

            if (sttLogs.isEmpty()) {
                log.warn("⚠️ STT 로그 없음 - CallSession ID: {}", callSessionId);
                throw new CallSessionSummaryGenerationException(ErrorStatus.CANT_SUMMARY_CALL_STT_LOG);
            }

            boolean hasMeaningfulScript = sttLogs.stream()
                    .anyMatch(log -> log.getScript() != null && !log.getScript().trim().isEmpty());

            if (!hasMeaningfulScript) {
                log.warn("⚠️ 유효한 대화 내용 없음 - CallSession ID: {}", callSessionId);
                throw new CallSessionSummaryGenerationException(ErrorStatus.CALL_STT_LOG_NO_MEANINGFUL_CONTENT);
            }

            String fullConversation = sttLogs.stream()
                    .filter(log -> {
                        String script = log.getScript();
                        return script != null && !script.trim().isEmpty();
                    })
                    .map(log -> {
                        String script = log.getScript().trim();
                        String speaker = (log.getTrack() == CallTrack.INBOUND) ? "고객" : "상담원";
                        return String.format("[%s]: %s", speaker, script);
                    })
                    .collect(Collectors.joining("\n"));

            String summary = openAiSummaryService.summarize(fullConversation);
            log.info("✅ 요약 생성 완료 - CallSession ID: {}", callSessionId);

            session.updateSummary(summary);
            session.updateSummaryGenerated(true);
            session.updateSummaryGeneratedAt(LocalDateTime.now());
            callSessionRepository.save(session);

            return summary;

        } catch (Exception e) {
            log.error("❌ 요약 생성 중 오류 - CallSession ID: {}, 메시지: {}", callSessionId, e.getMessage(), e);
            throw new CallSessionSummaryGenerationException(ErrorStatus.SUMMARY_AI_OPENAI_API_ERROR);
        }
    }

    @Override
    public CallSessionResponseDTO.CallSessionSummaryResponseDTO createCallSessionSummaryByOpenAi(Long callSessionId) {
        String summaryText = generateSummaryByOpenAi(callSessionId);

        return CallSessionResponseDTO.CallSessionSummaryResponseDTO.builder()
                .callSessionId(callSessionId)
                .summaryText(summaryText)
                .build();
    }

    @Override
    @Transactional
    public String generateGeminiSummary(Long callSessionId, Long userId) {
        CallSession session = findCallSessionByIdAndUserId(callSessionId, userId);

        // 중복 생성 방지
        if (session.getSummaryGemini() != null && !session.getSummaryGemini().isBlank()) {
            log.info("CallSession (ID: {})에 이미 요약 완료. Gemini api 호출 없이 기존 요약 내용 반환", callSessionId);
            return session.getSummaryGemini();
        }

        String summaryText;

        try {
            // 전체 스크립트 조회
            List<CallSttLog> scriptLogs = callSttLogService.getAllBySessionId(callSessionId);

            if (scriptLogs.isEmpty()) {
                log.warn("CallSession (ID: {})에 최종 STT 로그가 없어 요약을 생성할 수 없습니다.", callSessionId);
                throw new CallSessionSummaryGenerationException(ErrorStatus.CANT_SUMMARY_CALL_STT_LOG);
            }

            // 하나의 전체 스크립트로 가공
            String fullConversation = scriptLogs.stream()
                .map(log -> {
                    String speaker = log.getTrack().toString().equals("INBOUND") ? "고객" : "상담원";
                    return String.format("[%s]: %s", speaker, log.getScript());
                })
                .collect(Collectors.joining("\n"));

            boolean hasMeaningfulScript = scriptLogs.stream()
                .anyMatch(log -> log.getScript() != null && !log.getScript().trim().isEmpty());

            if (!hasMeaningfulScript) {
                log.warn("CallSession (ID: {})에 의미 있는 대화 내용이 없어 요약을 생성할 수 없습니다.", callSessionId);
                throw new CallSessionSummaryGenerationException(ErrorStatus.CALL_STT_LOG_NO_MEANINGFUL_CONTENT);
            }

            summaryText = geminiService.summarizeCallScript(fullConversation);
            log.info("CallSession (ID: {}) 요약 생성 완료.", callSessionId);

            session.updateSummaryGemini(summaryText);
            log.info("summaryGemini: {}", summaryText);
            callSessionRepository.save(session);
            return summaryText;

        } catch (Exception e) {
            log.error("CallSession (ID: {}) 요약 생성 중 오류 발생: {}", callSessionId, e.getMessage(), e);
            throw new CallSessionSummaryGenerationException(ErrorStatus.SUMMARY_AI_GEMINI_API_ERROR);
        }
    }

    @Override
    @Transactional
    public CallSessionResponseDTO.CallSessionSummaryResponseDTO createCallSessionSummary(Long callSessionId, Long userId) {
        String summaryText = generateGeminiSummary(callSessionId, userId);

        return CallSessionResponseDTO.CallSessionSummaryResponseDTO.builder()
            .callSessionId(callSessionId)
            .summaryText(summaryText)
            .build();
    }

    @Override
    @Transactional
    public void updateEndedAt(final Long callSessionId) {
        CallSession session = findCallSessionById(callSessionId);
        session.updateEndedAt();
        log.info("CallSession (ID: {})의 endedAt 업데이트 완료 : {}", callSessionId, session.getEndedAt());
    }

    private void validateAbuseCategory(String category) {
        List<String> valid = List.of("verbalAbuse", "sexualHarass", "threat");
        if (!valid.contains(category)) {
            throw new InvalidCategoryFilterException();
        }
    }

    private String getAbuseCategoryForSession(CallSession session) {
        List<CallLog> callLogs = callLogRepository.findByCallSession(session);

        for (CallLog callLog : callLogs) {
            List<AbuseLog> abuseLogs = abuseLogRepository.findByCallLog(callLog);
            for (AbuseLog abuseLog : abuseLogs) {
                List<AbuseTypeLog> typeLogs = abuseTypeLogRepository.findByAbuseLog(abuseLog);
                for (AbuseTypeLog typeLog : typeLogs) {
                    AbuseType type = typeLog.getAbuseType();
                    if (type.isVerbalAbuse()) return "폭언";
                    if (type.isSexualHarass()) return "성희롱";
                    if (type.isThreat()) return "협박";
                }
            }
        }
        return "전체";
    }

    // 검색어가 포함된 scrpit 일부만 추출하여 반환하는 함수
    private String extractSnippetAroundKeyword(String script, String keyword, int contextLength) {
        if (script == null || keyword == null || keyword.isBlank()) return null;

        int index = script.indexOf(keyword);
        if (index == -1) return null; // 검색어가 없으면 null 반환

        int start = index;
        int end = Math.min(script.length(), index + keyword.length() + contextLength);

        return script.substring(start, end).replaceAll("\\s+", " ").trim();
    }

    private CallSession findCallSessionById(final Long callSessionId) {
        return callSessionRepository.findById(callSessionId).orElseThrow(CallSessionNotFoundException::new);
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

    private CallSession findCallSessionByIdAndUserId(final Long callSessionId, final Long userId) {
        return callSessionRepository.findByIdAndUserId(callSessionId, userId).orElseThrow(CallSessionUserNotFoundException::new);
    }
}
