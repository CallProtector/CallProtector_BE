package callprotector.spring.repository;
import java.util.Optional;

import callprotector.spring.domain.User;
import callprotector.spring.domain.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findTopByEmailOrderByExpiresAtDesc(String email);
}
