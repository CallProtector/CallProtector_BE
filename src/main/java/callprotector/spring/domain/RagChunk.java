package callprotector.spring.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RagChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private RagDocs ragDocs;

    @Column(nullable = false)
    private String chunkText;

    @Lob
    @Column(nullable = false)
    private String embeddingVector;

    @Column(nullable = false)
    private Long chunkIndex;

}

