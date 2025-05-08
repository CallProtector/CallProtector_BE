package callprotector.spring.domain;

import callprotector.spring.domain.enums.LegalCategory;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RagDocs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LegalCategory legalCategory;

    @Column(nullable = false)
    private String docsTitle;

    @Lob
    @Column(nullable = false)
    private String docsContent;

    @Lob
    @Column(nullable = false)
    private String sourceUrl;

    @OneToMany(mappedBy = "ragDocs", cascade = CascadeType.ALL)
    private List<RagChunk> chunks = new ArrayList<>();

}

