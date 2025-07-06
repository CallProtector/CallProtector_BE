package callprotector.spring.web.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class CallSessionRequestDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallSessionMakeDTO{
        private Long userId;      // 사용자 ID
        private String title;     // 상담 제목
        private String twilioCallSid; // Twilio Call SID
    }

}
