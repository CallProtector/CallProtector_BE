package callprotector.spring.web.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import callprotector.spring.domain.CallSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class CallSessionResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallSessionMakeDTO{
        private Long sessionId;
        private String callSessionCode;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallSessionInfoDTO {
        private String callSessionCode;
        private String createdAt;
        private Integer totalAbuseCnt;
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
        // private String aiSummary;
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

        public static CallSessionListDTO fromEntity(CallSession session) {
            return CallSessionListDTO.builder()
                    .id(session.getId())
                    .callSessionCode(session.getCallSessionCode())
                    .callerNumber(session.getCallerNumber())
                    .createdAt(session.getCreatedAt())
                    .build();
        }
    }
}
