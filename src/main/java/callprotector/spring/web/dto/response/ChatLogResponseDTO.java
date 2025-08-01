package callprotector.spring.web.dto.response;

import lombok.*;

import java.util.List;

public class ChatLogResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatLogResponse {
        private Long id;
        private String question;
        private String answer;
        private List<String> sourcePages;
        private String createdAt;
    }


}
