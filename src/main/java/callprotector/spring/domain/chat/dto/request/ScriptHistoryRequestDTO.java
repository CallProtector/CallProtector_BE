package callprotector.spring.domain.chat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ScriptHistoryRequestDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScriptHistoryDTO{
        private String speaker; // INBOUND/OUTBOUND
        private String text; // 대화 내용
    }


}
