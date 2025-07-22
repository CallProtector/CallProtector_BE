package callprotector.spring.repository;

import callprotector.spring.domain.CallSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallSessionRepository extends JpaRepository<CallSession, Long>, CallSessionRepositoryCustom {
    long countByCallSessionCodeStartingWith(String prefix);
}