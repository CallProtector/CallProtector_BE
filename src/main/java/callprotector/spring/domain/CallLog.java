package callprotector.spring.domain;

import callprotector.spring.domain.common.BaseEntity;
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
    @Column(nullable = false, length = 20)
    private String audio_url;

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

    // 크기가 큰 ERD-TEXT 속성은 크기 설정 안 함
    @Setter
    @Column(nullable = false)
    private String summary;

}
