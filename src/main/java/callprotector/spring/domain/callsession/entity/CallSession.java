package callprotector.spring.domain.callsession.entity;

import callprotector.spring.domain.user.entity.User;
import callprotector.spring.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CallSession extends BaseEntity {

    private static final Integer DEFAULT_ABUSE_COUNT = 0;
    private static final Boolean DEFAULT_ABUSE_TAG = Boolean.FALSE;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 날짜 기반 세션 코드 (예: 20250531-0001)
    @Column(nullable = false, unique = true, length = 20)
    private String callSessionCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="user_id")
    private User user;

    @Column(length = 50)
    private String callerNumber;

    @Builder.Default
    private Boolean abuseTag = DEFAULT_ABUSE_TAG;

    // 통화 시작 시간은 (createdAt 으로 대체)

    // 통화 종료 시간 (통화가 끝나지 않은 경우를 생각하기 위해 true 지정)
    @Column(nullable = true)
    private LocalDateTime endedAt;

    @Column(name = "total_abuse_cnt")
    @Builder.Default
    private Integer totalAbuseCnt = DEFAULT_ABUSE_COUNT;

    // CA로 시작하는 34자리 문자열
    @Column(name = "twilio_call_sid", length = 34)
    private String twilioCallSid;

    // 상담 요약
    @Column(name= "summary_simple",length = 2000)
    private String summarySimple;

    @Column(name= "summary_detailed", length = 2000)
    private String summaryDetailed;

    // 통화 강제 종료 여부
    @Column(nullable = false)
    private boolean forcedTerminated = false;

    public void updateAbuseCnt() {
        this.totalAbuseCnt = (this.totalAbuseCnt == null ? 0 : this.totalAbuseCnt) + 1;
    }

    public void updateEndedAt() {
        this.endedAt = LocalDateTime.now();
    }

    public void updateAbuseTag() {
        this.abuseTag = Boolean.TRUE;
    }

    public void updateSummarySimple(String summarySimple) {
        this.summarySimple = summarySimple;
    }

    public void updateSummaryDetailed(String summaryDetailed) {
        this.summaryDetailed = summaryDetailed;
    }

    public void updateUser(User user) {
        this.user = user;
    }

    public void markForcedTerminated() { this.forcedTerminated = true; }

}
