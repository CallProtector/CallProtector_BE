package callprotector.spring.domain.mapping;

import callprotector.spring.domain.LegalBotResponse;
import callprotector.spring.domain.RagChunk;
import callprotector.spring.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ResChunk extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "res_id", nullable = false)
    private LegalBotResponse response;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chunk_id", nullable = false)
    private RagChunk chunk;

}
