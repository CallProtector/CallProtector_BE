package callprotector.spring.domain.abuse.repository;

import callprotector.spring.domain.abuse.entity.AbuseLog;
import callprotector.spring.domain.calllog.entity.CallLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AbuseLogRepository extends JpaRepository<AbuseLog, Long> {
    List<AbuseLog> findByCallLog(CallLog callLog);
}