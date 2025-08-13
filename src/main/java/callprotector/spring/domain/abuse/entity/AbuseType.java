package callprotector.spring.domain.abuse.entity;

import callprotector.spring.global.common.entity.BaseEntity;
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

    @Builder.Default
    @OneToMany(mappedBy = "abuseType", cascade = CascadeType.ALL)
    private List<AbuseTypeLog> abuseTypeLogList = new ArrayList<>();

    @Column(nullable = false)
    private boolean verbalAbuse;

    @Column(nullable = false)
    private boolean sexualHarass;

    @Column(nullable = false)
    private boolean threat;

}
