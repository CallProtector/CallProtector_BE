package callprotector.spring.domain.callsession.repository;

import java.util.Optional;

import callprotector.spring.domain.callsession.entity.CallSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CallSessionRepository extends JpaRepository<CallSession, Long>, CallSessionRepositoryCustom {
    long countByCallSessionCodeStartingWith(String prefix);
    Optional<CallSession> findByIdAndUserId(Long id, Long userId);
    boolean existsByTwilioCallSid(String twilioCallsid);
    Optional<CallSession> findByTwilioCallSid(String twilioCallsid);

    @Query("SELECT c.twilioConferenceSid FROM CallSession c WHERE c.id = :callSessionId")
    Optional<String> findConferenceSidById(@Param("callSessionId") Long callSessionId);
}