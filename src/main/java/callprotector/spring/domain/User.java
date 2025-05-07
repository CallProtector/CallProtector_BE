package callprotector.spring.domain;

import callprotector.spring.domain.enums.Department;
import callprotector.spring.domain.enums.Position;
import callprotector.spring.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

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

    @Column(nullable = false, length = 16)
    private String password;

    private String profileImg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Position position;

    @Column(nullable = false)
    private Long totalCall = 0L;

    @Column(nullable = false, length = 15)
    private Long phoneNumber;

    @LastModifiedDate
    private LocalDateTime updatedAt;

}