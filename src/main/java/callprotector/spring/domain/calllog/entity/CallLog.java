package callprotector.spring.domain.calllog.entity;

import callprotector.spring.domain.callsession.entity.CallSession;
import callprotector.spring.global.common.entity.BaseEntity;
import callprotector.spring.global.common.enums.CallTrack;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    private String audio_url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallTrack track;

    @Column(nullable = false)
    private String script;

    @Column(name = "abuse_cnt")
    private Integer abuseCnt;

    @Column(name = "abuse_detect")
    private Boolean abuseDetect;

    public void updateScript(String script) {
        this.script = script;
    }
}
