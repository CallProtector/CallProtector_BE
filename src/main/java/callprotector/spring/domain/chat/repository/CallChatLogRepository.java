package callprotector.spring.domain.chat.repository;

import callprotector.spring.domain.chat.entity.CallChatLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CallChatLogRepository extends JpaRepository<CallChatLog, Long> {
    @Query("SELECT l FROM CallChatLog l WHERE l.callChatSession.id = :sessionId ORDER BY l.createdAt ASC")
    List<CallChatLog> findBySessionIdWithOrder(@Param("sessionId") Long sessionId);
}
