package callprotector.spring.web.dto.response;

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
}
