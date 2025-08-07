package callprotector.spring.domain.callsession.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import callprotector.spring.domain.callsession.entity.CallSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class CallSessionResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallSessionInfoDTO {
        private String callSessionCode;
        private String createdAt;
        private Integer totalAbuseCnt;
        private Long callSessionId;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallSessionTotalAbuseCntDTO {
        private Long sessionId;
        private Integer totalAbuseCnt;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallSessionScriptDTO {
        private String id;
        private Long callSessionId;
        private String speaker;
        private String text;
        private Boolean isAbuse;
        private String abuseType;
        private LocalDateTime timestamp;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallSessionDetailResponseDTO {
        private CallSessionInfoDTO sessionInfo; // 내부 DTO 사용
        private List<CallSessionScriptDTO> scriptHistory;
        private CallSessionAISummariesDTO aiSummary;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CallSessionListDTO {
        private Long id;
        private String callSessionCode;
        private String callerNumber;
        private LocalDateTime createdAt;
        private String category;

        private String matchedScript;

        public static CallSessionListDTO fromEntity(CallSession session, String category) {
            return CallSessionListDTO.builder()
                    .id(session.getId())
                    .callSessionCode(session.getCallSessionCode())
                    .callerNumber(session.getCallerNumber())
                    .createdAt(session.getCreatedAt())
                    .category(category)
                    .build();
        }

        // 검색 사용 시 응답
        public static CallSessionListDTO fromEntityWithScript(CallSession session, String category, String matchedScript) {
            return CallSessionListDTO.builder()
                    .id(session.getId())
                    .callSessionCode(session.getCallSessionCode())
                    .callerNumber(session.getCallerNumber())
                    .createdAt(session.getCreatedAt())
                    .category(category)
                    .matchedScript(matchedScript)
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AbusiveCallSessionDTO {
        private Long id;
        private String callSessionCode;
        private LocalDateTime createdAt;
        private String category;

        public static AbusiveCallSessionDTO fromEntity(CallSession session, String category) {
            return AbusiveCallSessionDTO.builder()
                    .id(session.getId())
                    .callSessionCode(session.getCallSessionCode())
                    .createdAt(session.getCreatedAt())
                    .category(category)
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CallSessionPagingDTO {
        private List<CallSessionListDTO> sessions;
        private Long nextCursorId;
        private boolean hasNext;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AbusiveCallSessionPagingDTO {
        private List<AbusiveCallSessionDTO> sessions;
        private Long nextCursorId;
        private boolean hasNext;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallSessionSummaryResponseDTO {
        private Long callSessionId;
        private String summaryText;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallSessionAISummariesDTO {
        private String simple;
        private String detailed;
    }
}
