package callprotector.spring.domain.abuse.entity;

import callprotector.spring.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AbuseTypeLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "abuse_log_id")
    private AbuseLog abuseLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "abuse_type_id")
    private AbuseType abuseType;


}
