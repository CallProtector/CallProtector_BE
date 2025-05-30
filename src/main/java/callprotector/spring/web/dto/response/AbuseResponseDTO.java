package callprotector.spring.web.dto.response;

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

        // Java에서 생성자의 이름은 반드시 클래스 이름과 같아야 한다!!!!!
        // ㅁㅊ.. 기본을 까먹다니;;
        // AbuseFilterDTO로 함수 이름 수정 간다. -> @AllArgsConstructor 가 다 해주잖


        public boolean isAbuse() { return abuse; };

        public boolean isDetected() { return detected;}

        public String getType() { return type; }



    }


}
