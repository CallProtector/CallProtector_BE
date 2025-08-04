package callprotector.spring.domain.callsttlog.repository;

import callprotector.spring.domain.callsttlog.entity.CallSttLog;

import java.util.List;

public interface CallSttLogSearchRepository {

    List<CallSttLog> searchByKeywordAndFilters(
            String keyword,
            String category,
            String order,
            Long cursorId,
            int size
    );

}