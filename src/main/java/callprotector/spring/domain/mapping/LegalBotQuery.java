package callprotector.spring.domain.mapping;

import callprotector.spring.domain.CallSession;
import callprotector.spring.domain.ChatSession;
import callprotector.spring.domain.User;
import callprotector.spring.domain.common.BaseEntity;
import callprotector.spring.domain.enums.BotKind;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class LegalBotQuery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id", nullable = false)
    private ChatSession chatSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_session_id")
    private CallSession callSession;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private Long botNumber;

    @Column(nullable = false)
    private String queryTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BotKind botKind;

}

