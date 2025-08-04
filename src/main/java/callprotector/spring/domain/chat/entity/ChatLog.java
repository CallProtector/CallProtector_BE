package callprotector.spring.domain.chat.entity;

import callprotector.spring.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ChatLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 세션 연관 (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id")
    private ChatSession chatSession;  // 세션 정보

    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(columnDefinition = "TEXT")
    private String sourcePages;



    // ++ 필요한 경우 명시적으로 생성자/팩토리 메서드 추가도 가능
    public static ChatLog of(ChatSession session, String question, String answer, List<String> sourcePages) {
        return ChatLog.builder()
                .chatSession(session)
                .question(question)
                .answer(answer)
                .build();
    }



}
