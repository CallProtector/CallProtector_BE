package callprotector.spring.domain.chat.repository;

import callprotector.spring.domain.chat.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

}
