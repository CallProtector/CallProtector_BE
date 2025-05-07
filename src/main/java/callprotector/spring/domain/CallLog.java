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

    @Column(nullable = false, length = 20)
    private String audio_url;

    // 크기가 큰 ERD-TEXT 속성은 크기 설정 안 함
    @Column(nullable = false)
    private String script;

    @Column(nullable = false)
    private Integer abuse_cnt;

    private Boolean abuse_detect;

    // 크기가 큰 ERD-TEXT 속성은 크기 설정 안 함
    @Column(nullable = false)
    private String summary;

}
