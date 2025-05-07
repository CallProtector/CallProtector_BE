package callprotector.spring.domain;

import callprotector.spring.domain.common.BaseEntity;
import callprotector.spring.domain.mapping.AbuseTypeLog;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AbuseType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "abuseType", cascade = CascadeType.ALL)
    private List<AbuseTypeLog> abuseTypeLogList = new ArrayList<>();

    @Column(nullable = false, length = 8)
    private String verbalAbuse;

    @Column(nullable = false, length = 8)
    private String sexualHarass;

    @Column(nullable = false, length = 8)
    private String threat;


}
