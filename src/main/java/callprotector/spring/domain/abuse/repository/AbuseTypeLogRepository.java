package callprotector.spring.domain.abuse.repository;

import callprotector.spring.domain.abuse.entity.AbuseLog;
import callprotector.spring.domain.abuse.entity.AbuseTypeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AbuseTypeLogRepository extends JpaRepository<AbuseTypeLog, Long> {
    List<AbuseTypeLog> findByAbuseLog(AbuseLog abuseLog);
}