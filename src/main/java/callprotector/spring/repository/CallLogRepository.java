package callprotector.spring.repository;

import callprotector.spring.domain.CallLog;
import callprotector.spring.domain.CallSession;
import callprotector.spring.domain.enums.CallTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CallLogRepository extends JpaRepository<CallLog, Long> {

    Optional<CallLog> findByCallSessionAndTrack(CallSession callSession, CallTrack track);

    List<CallLog> findByCallSession(CallSession session);
}
