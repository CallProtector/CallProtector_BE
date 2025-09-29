package callprotector.spring.domain.callchat.repository;

import callprotector.spring.domain.callchat.entity.CallChatSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CallChatSessionRepository extends JpaRepository<CallChatSession, Long> {
    @Query("""
        select cs
        from CallChatSession cs
        where cs.user.id = :userId
        order by 
            case when cs.lastUserQuestionAt is null then 1 else 0 end,
            cs.lastUserQuestionAt desc,
            cs.id desc
        """)
    List<CallChatSession> findAllForUserOrderByLastUserQuestion(@Param("userId")Long userId);

    // CallChatSessionRepository
    Optional<CallChatSession> findByUserIdAndCallSessionId(Long userId, Long callSessionId);

}
