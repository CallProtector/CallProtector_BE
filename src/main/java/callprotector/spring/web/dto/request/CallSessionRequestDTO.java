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
        private String originalInboundCallSid; // client -> twilio
        private String callerNumber;
    }

}
