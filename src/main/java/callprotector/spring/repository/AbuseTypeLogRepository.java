package callprotector.spring.repository;

import callprotector.spring.domain.mapping.AbuseTypeLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AbuseTypeLogRepository extends JpaRepository<AbuseTypeLog, Long> {}