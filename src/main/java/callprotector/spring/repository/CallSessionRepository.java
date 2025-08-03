package callprotector.spring.repository;

import java.util.Optional;

import callprotector.spring.domain.CallSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallSessionRepository extends JpaRepository<CallSession, Long>, CallSessionRepositoryCustom {
    long countByCallSessionCodeStartingWith(String prefix);
    Optional<CallSession> findByIdAndUserId(Long id, Long userId);
    boolean existsByTwilioCallSid(String twilioCallsid);
}