package callprotector.spring.repository;

import callprotector.spring.domain.AbuseLog;
import callprotector.spring.domain.mapping.AbuseTypeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AbuseTypeLogRepository extends JpaRepository<AbuseTypeLog, Long> {
    List<AbuseTypeLog> findByAbuseLog(AbuseLog abuseLog);
}