package callprotector.spring.domain;

import callprotector.spring.domain.common.BaseEntity;
import callprotector.spring.domain.mapping.AbuseTypeLog;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AbuseLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_log_id")
    private CallLog callLog;

    @OneToMany(mappedBy = "abuseLog", cascade = CascadeType.ALL)
    private List<AbuseTypeLog> abuseTypeLogList = new ArrayList<>();

    private LocalDateTime detectedAt;


}
