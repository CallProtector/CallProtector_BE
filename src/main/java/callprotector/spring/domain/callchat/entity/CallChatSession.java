package callprotector.spring.domain.callchat.entity;

import callprotector.spring.domain.callsession.entity.CallSession;
import callprotector.spring.domain.user.entity.User;
import callprotector.spring.global.common.entity.BaseEntity;
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
@Table(
        name = "call_chat_session",
        indexes = {
                @Index(name = "idx_call_chat_session_user_last_question", columnList = "user_id,last_user_question_at")
        }
)
public class CallChatSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private CallSession callSession;

    private String title;

    @OneToMany(mappedBy = "callChatSession", cascade = CascadeType.ALL)
    private List<CallChatLog> logs = new ArrayList<>();

    // 마지막 사용자 질문 시각(denorm)
    @Column(name = "last_user_question_at")
    private LocalDateTime lastUserQuestionAt;

    public void touchLastUserQuestionAt(LocalDateTime t) {
        this.lastUserQuestionAt = t;
    }
}
