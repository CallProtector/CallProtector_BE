package callprotector.spring.domain.chat.entity;

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
        name = "chat_session",
        indexes = {
                @Index(name = "idx_chat_session_user_last_question", columnList = "user_id,last_user_question_at")
        }
)
public class ChatSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer status; // 1. 진행 중, 2:종료

    @Setter
    private String title;

    @Builder.Default
    @OneToMany(mappedBy = "chatSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatLog> chatLogs = new ArrayList<>();

    // 마지막 사용자 질문 시각(denorm)
    @Column(name = "last_user_question_at")
    private LocalDateTime lastUserQuestionAt;

    public void touchLastUserQuestionAt(LocalDateTime t) {
        this.lastUserQuestionAt = t;
    }


}

