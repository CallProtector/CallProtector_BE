package callprotector.spring.domain;

import callprotector.spring.domain.common.BaseEntity;
import callprotector.spring.domain.mapping.LegalBotQuery;
import callprotector.spring.domain.mapping.ResChunk;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class LegalBotResponse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id", nullable = false)
    private LegalBotQuery query;

    @Lob
    @Column(nullable = false)
    private String resContent;

    @Lob
    @Column(nullable = false)
    private String sourceDocs;

    @OneToMany(mappedBy = "response", cascade = CascadeType.ALL)
    private List<ResChunk> resChunks = new ArrayList<>();

}
