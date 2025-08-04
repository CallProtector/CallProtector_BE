package callprotector.spring.domain.abuse.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AbuseRequestDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbuseFilterDTO{
        private String text;

        public String getText() { return text; }
        public void setText(String text) { this.text = text;}
    }

}
