package callprotector.spring.repository;

import callprotector.spring.domain.CallSession;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CallSessionRepositoryImpl implements CallSessionRepositoryCustom {

    @PersistenceContext
    private final EntityManager em;

    @Override
    public List<CallSession> findFirstPage(String sortBy, int limit, Sort.Direction direction) {
        String jpql = "SELECT c FROM CallSession c ORDER BY c." + sortBy + " " + direction.name();
        return em.createQuery(jpql, CallSession.class)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public List<CallSession> findByCursor(String sortBy, Object cursorValue, int limit, Sort.Direction direction) {
        String operator = direction.isAscending() ? ">" : "<";

        String jpql = "SELECT c FROM CallSession c " +
                "WHERE c." + sortBy + " " + operator + " :cursorValue " +
                "ORDER BY c." + sortBy + " " + direction.name();

        return em.createQuery(jpql, CallSession.class)
                .setParameter("cursorValue", cursorValue)
                .setMaxResults(limit)
                .getResultList();
    }
}