package callprotector.spring.domain;

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


    @OneToMany(mappedBy = "chatSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatLog> chatLogs = new ArrayList<>();


}

