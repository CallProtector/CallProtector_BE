package callprotector.spring.domain.chat.repository;

import callprotector.spring.domain.chat.entity.ChatLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatLogRepository extends JpaRepository<ChatLog, Long> {
    List<ChatLog> findAllByChatSessionId(Long chatSessionId);
}
