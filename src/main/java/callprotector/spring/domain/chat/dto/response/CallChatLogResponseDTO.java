package callprotector.spring.domain.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class CallChatLogResponseDTO {


    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallChatLogResponse {
        private Long id;
        private String question;
        private String answer;
        private List<SourcePageDTO.SourcePage> sourcePages;
        private String createdAt;
    }
}
