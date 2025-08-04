package callprotector.spring.domain.user.repository;

import callprotector.spring.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findById(Long id);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
}
