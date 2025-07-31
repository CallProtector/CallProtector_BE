package callprotector.spring.repository;

import callprotector.spring.domain.CallSttLog;

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