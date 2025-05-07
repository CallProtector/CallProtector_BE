package callprotector.spring.domain;

import callprotector.spring.domain.common.BaseEntity;
import callprotector.spring.domain.mapping.LegalBotQuery;
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
public class ChatSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Column(nullable = false)
    private Integer status;

    @OneToMany(mappedBy = "chatSession")
    private List<LegalBotQuery> legalBotQueries = new ArrayList<>();

}

