package callprotector.spring.domain.callchat.repository;

import callprotector.spring.domain.callchat.entity.CallChatSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CallChatSessionRepository extends JpaRepository<CallChatSession, Long> {
    List<CallChatSession> findByUserIdOrderByIdDesc(Long userId);

    // CallChatSessionRepository
    Optional<CallChatSession> findByUserIdAndCallSessionId(Long userId, Long callSessionId);

}
