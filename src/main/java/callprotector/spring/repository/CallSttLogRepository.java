package callprotector.spring.repository;

import callprotector.spring.domain.CallSttLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CallSttLogRepository extends MongoRepository<CallSttLog, String> {
}
