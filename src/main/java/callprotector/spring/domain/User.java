package callprotector.spring.domain;

import callprotector.spring.domain.enums.Department;
import callprotector.spring.domain.enums.Position;
import callprotector.spring.domain.common.BaseEntity;
import callprotector.spring.domain.mapping.LegalBotQuery;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;

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

    @Column(nullable = false, length = 20, unique = true)
    private String email;

    //@Column(nullable = false, length = 16) <- 에러 방지 위해, 일단 주석 : 당장 회원가입 시 사용하지 않기 때문에
    private String password;

    private String profileImg;

    @Enumerated(EnumType.STRING)
    // @Column(nullable = false) <- 에러  방지 위해, 일단 주석
    private Department department;

    @Enumerated(EnumType.STRING)
    // @Column(nullable = false) <- 에러  방지 위해,  일단 주석
    private Position position;

    //@Column(nullable = false) <- 에러 방지 위해,  일단 주석
    private Long totalCall = 0L;

    @Column(nullable = false, length = 15)
    private Long phoneNumber;

    //@LastModifiedDate <- 에러 방지 위해,  일단 주석
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<ChatSession> chatSessions = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<LegalBotQuery> legalBotQueries = new ArrayList<>();

}