package callprotector.spring.domain.chat.dto.response;

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
        private List<SourcePageDTO.SourcePage> sourcePages; // 문자열 대신 리스트로 수정
        private String createdAt;
    }


}
