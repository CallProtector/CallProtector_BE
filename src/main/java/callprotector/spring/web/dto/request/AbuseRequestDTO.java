package callprotector.spring.web.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AbuseRequestDTO {


    @Builder // DTO에도 빌더 패턴 쓴다? -> 그냥 우리가 만드는 인스턴스들은 모두 빌더 패턴 사용한다고 보면 됨
             // 참고로 RequestDTO는 우리가 만드는 것이 아닌, 프론트엔드에서 만든 객체를 그저 받기에, 빌더 패턴 적용할 필요 X
    @Getter
    @NoArgsConstructor // 기본 생성자 초기화
    @AllArgsConstructor // 모든 필드 초기화
    public static class AbuseFilterDTO{
        // public static class 쓰는 이유:
        // - DTO들은 저렇게 큰 묶음으로 (멤버 관련 DTO등등..) 클래스를 만들고
        // - 내부적으로 static 클래스를 만드는 것이 좋습니다.
        // DTO 자체는 수많은 곳에서 사용이 될 수 있기에 static class 로 만들게 되면,
        // 매번 class 파일을 만들 필요도 없고, 범용적으로 DTO를 사용할 수 있습니다.

        private String text;

        public String getText() { return text; }
        public void setText(String text) { this.text = text;}

    }
}
