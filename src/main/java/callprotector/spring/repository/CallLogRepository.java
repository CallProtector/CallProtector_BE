package callprotector.spring.repository;

import callprotector.spring.domain.CallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CallLogRepository extends JpaRepository<CallLog, Long> {

    // call_session_id 기준으로 CallLog 엔티티 조회
    Optional<CallLog> findByCallSession_Id(Long callSessionId);

}
