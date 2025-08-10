package callprotector.spring.domain.chat.entity;

import callprotector.spring.domain.callsession.entity.CallSession;
import callprotector.spring.domain.user.entity.User;
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
}
