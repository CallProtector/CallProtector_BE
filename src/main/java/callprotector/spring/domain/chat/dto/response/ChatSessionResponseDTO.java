package callprotector.spring.domain.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ChatSessionResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatSessionResponse{
        private Long sessionId;
        private String startTime;
        private String title;
        private String category;
        private String lastUserQuestionAt;
    }

}
