package callprotector.spring.domain.chat.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class SessionDetailResponseDTO {

    private SessionInfoDTO sessionInfoDTO;
    private List<ScriptHistoryDTO> scriptHistoryDTOList;
    private String aiSummary;

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class  SessionInfoDTO {
        private String callSessionCode; // 예: "20250805-0001"
        private String createdAt; // 예: "8.5 (화) 21:10"
        private int totalAbuseCnt; // 욕설 횟수
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScriptHistoryDTO {
        private String id; // MonogDB ObjectId
        private Long callSessionId; // 세션 ID
        private String speaker; // INBOUND / OUTBOUND
        private String text; // 대화 내용
        private boolean isAbuse; // 욕설 여부
        private String abuseType; // 욕설 유형 (폭언, 성희롱, 협박 등)
        private String timestamp; // 발화 시각
    }



}
