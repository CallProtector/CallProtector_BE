package callprotector.spring.repository;

import callprotector.spring.domain.AbuseType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AbuseTypeRepository extends JpaRepository<AbuseType, Long> {}