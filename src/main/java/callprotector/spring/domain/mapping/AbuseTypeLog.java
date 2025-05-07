package callprotector.spring.domain.mapping;

import callprotector.spring.domain.AbuseLog;
import callprotector.spring.domain.AbuseType;
import callprotector.spring.domain.CallLog;
import callprotector.spring.domain.common.BaseEntity;
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
