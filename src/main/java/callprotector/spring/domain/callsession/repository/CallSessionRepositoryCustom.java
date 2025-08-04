package callprotector.spring.domain.callsession.repository;

import callprotector.spring.domain.callsession.entity.CallSession;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface CallSessionRepositoryCustom {
    List<CallSession> findFirstPage(String sortBy, int limit, Sort.Direction direction);
    List<CallSession> findByCursor(String sortBy, Object cursorValue, int limit, Sort.Direction direction);

    List<CallSession> findSessionsByAbuseCategory(String category, Long cursorId, int limit, Sort.Direction direction);
    List<CallSession> findByIdsWithOrder(List<Long> ids, Sort.Direction direction);
}
