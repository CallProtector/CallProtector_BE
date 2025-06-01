package callprotector.spring.repository;

import callprotector.spring.domain.AbuseLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AbuseLogRepository extends JpaRepository<AbuseLog, Long> {}