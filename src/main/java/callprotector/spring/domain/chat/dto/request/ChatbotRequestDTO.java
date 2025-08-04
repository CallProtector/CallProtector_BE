package callprotector.spring.domain.chat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// request 폴더 살리려고 일단 냅둠
public class ChatbotRequestDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatbotRequest {
        private Long sessionId;
        private String question;
    }
}
