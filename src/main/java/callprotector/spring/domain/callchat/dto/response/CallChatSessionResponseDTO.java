package callprotector.spring.domain.callchat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class CallChatSessionResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallChatSessionResponse{
        private Long sessionId;
        private String createdAt;
        private String title;
        private String lastUserQuestionAt;
    }

}
