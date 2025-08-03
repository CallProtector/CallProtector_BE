package callprotector.spring.web.dto.response;

import lombok.*;

import java.util.List;
import java.util.Map;

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
