package callprotector.spring.domain.chat.repository;

import callprotector.spring.domain.chat.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    @Query("""
        select cs
        from ChatSession cs
        where cs.user.id = :userId
        order by
            case when cs.lastUserQuestionAt is null then 1 else 0 end,
            cs.lastUserQuestionAt desc,
            cs.id desc
        """)
    List<ChatSession> findAllForUserOrderByLastUserQuestion(@Param("userId") Long userId);
}
