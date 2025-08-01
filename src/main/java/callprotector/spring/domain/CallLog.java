package callprotector.spring.domain;

import callprotector.spring.domain.common.BaseEntity;
import callprotector.spring.domain.enums.CallTrack;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CallLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_session_id")
    private CallSession callSession;

    @Setter
    private String audio_url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallTrack track;

    // 크기가 큰 ERD-TEXT 속성은 크기 설정 안 함
    @Setter
    @Column(nullable = false)
    private String script;

    @Setter
    @Column(name = "abuse_cnt")
    private Integer abuseCnt;

    @Setter
    @Column(name = "abuse_detect")
    private Boolean abuseDetect;

    public void updateScript(String script) {
        this.script = script;
    }
}
