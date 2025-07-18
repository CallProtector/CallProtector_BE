package callprotector.spring.repository;

import callprotector.spring.domain.AbuseLog;
import callprotector.spring.domain.CallLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AbuseLogRepository extends JpaRepository<AbuseLog, Long> {
    List<AbuseLog> findByCallLog(CallLog callLog);
}