package callprotector.spring.domain.abuse.repository;

import callprotector.spring.domain.abuse.entity.AbuseType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AbuseTypeRepository extends JpaRepository<AbuseType, Long> {}