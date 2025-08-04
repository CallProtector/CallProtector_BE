package callprotector.spring.domain.abuse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AbuseResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbuseFilterDTO{
        private boolean abuse;
        private boolean detected;
        private String type;

        public boolean isAbuse() { return abuse; };

        public boolean isDetected() { return detected;}

        public String getType() { return type; }
    }

}
