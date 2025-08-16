package callprotector.spring.domain.callchat.dto.response;

import callprotector.spring.domain.chat.dto.response.SourcePageDTO;
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

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallChatLogListResponse {
        private Long callSessionId;
        private List<CallChatLogResponseDTO.CallChatLogResponse> logs;
    }
}
