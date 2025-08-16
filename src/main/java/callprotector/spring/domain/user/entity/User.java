package callprotector.spring.domain.user.entity;

import callprotector.spring.domain.chat.entity.ChatSession;
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
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false, length = 200, unique = true)
    private String email;

    //@Column(nullable = false, length = 16) <- 에러 방지 위해, 일단 주석 : 당장 회원가입 시 사용하지 않기 때문에
    private String password;

    @Column(nullable = false, length = 15)
    private String phoneNumber;

    //@LastModifiedDate <- 에러 방지 위해,  일단 주석
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<ChatSession> chatSessions = new ArrayList<>();

}