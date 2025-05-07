package callprotector.spring.domain;

import callprotector.spring.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CallSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 지연로딩
    @JoinColumn(name ="user_id")
    private User user;

    @Column(nullable = false, length = 20)
    private String title;

    private Boolean abuseTag;

    // 통화 시작 시간은 (createdAt 으로 대체)

    // 통화 종료 시간 (통화가 끝나지 않은 경우를 생각하기 위해 true 지정)
    @Column(nullable = true)
    private LocalDateTime endedAt;

}
