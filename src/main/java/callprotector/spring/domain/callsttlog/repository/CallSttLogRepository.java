package callprotector.spring.domain.callsttlog.repository;

import callprotector.spring.domain.callsttlog.entity.CallSttLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CallSttLogRepository extends MongoRepository<CallSttLog, String> {
    boolean existsByCallSessionIdAndIsAbuseTrue(Long callSessionId);
    List<CallSttLog> findByCallSessionIdAndIsAbuseTrue(Long callSessionId);
    List<CallSttLog> findByCallSessionIdAndIsFinalOrderByTimestampAsc(Long callSessionId, Boolean isFinal);

}
