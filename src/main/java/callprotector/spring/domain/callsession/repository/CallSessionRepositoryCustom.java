package callprotector.spring.domain.callsession.repository;

import callprotector.spring.domain.callsession.entity.CallSession;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface CallSessionRepositoryCustom {
    List<CallSession> findFirstPageByUserId(Long userId, String sortBy, int limit, Sort.Direction direction);
    List<CallSession> findByUserIdAndCursor(Long userId, String sortBy, Object cursorValue, int limit, Sort.Direction direction);

    List<CallSession> findSessionsByAbuseCategoryAndUserId(String category, Long userId, Long cursorId, int limit, Sort.Direction direction);
    List<CallSession> findByIdsWithOrderAndUserId(List<Long> ids, Sort.Direction direction, Long userId);

    List<CallSession> findAbusiveCallSessions(Long userId, Long cursorId, int limit);
}
