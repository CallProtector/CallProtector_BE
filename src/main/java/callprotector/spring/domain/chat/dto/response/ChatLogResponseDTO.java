package callprotector.spring.domain.chat.dto.response;

import lombok.*;

public class ChatLogResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatLogResponse {
        private Long id;
        private String question;
        private String answer;
        private String sourcePages;
        private String createdAt;
    }


}
