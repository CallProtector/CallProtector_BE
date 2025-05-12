package callprotector.spring.repository;

import callprotector.spring.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findById(Long id);

    // 일단 이메일 중복 체크 메서드 추가추가
    boolean existsByEmail(String email);

    // 이메일로 사용자 조회
    Optional<User> findByEmail(String email);

    // userId를 추가해야되나 고민이네.. ㅇㅅㅇ
    // Optional<User> findByUserId(String userId);
}
