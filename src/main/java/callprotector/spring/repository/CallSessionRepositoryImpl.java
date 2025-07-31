package callprotector.spring.repository;

import callprotector.spring.domain.CallSession;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
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

    public List<CallSession> findSessionsByAbuseCategory(String category, Long cursorId, int limit, Sort.Direction direction) {
        String operator = direction.isAscending() ? ">" : "<";

        String jpql = "SELECT DISTINCT cs FROM CallSession cs " +
                "JOIN CallLog cl ON cl.callSession = cs " +
                "JOIN AbuseLog al ON al.callLog = cl " +
                "JOIN AbuseTypeLog atl ON atl.abuseLog = al " +
                "JOIN AbuseType at ON at = atl.abuseType " +
                "WHERE at." + category + " = true " +
                (cursorId != null ? "AND cs.id " + operator + " :cursorId " : "") +
                "ORDER BY cs.id " + direction.name();

        TypedQuery<CallSession> query = em.createQuery(jpql, CallSession.class)
                .setMaxResults(limit + 1);

        if (cursorId != null) {
            query.setParameter("cursorId", cursorId);
        }

        return query.getResultList();
    }

    @Override
    public List<CallSession> findByIdsWithOrder(List<Long> ids, Sort.Direction direction) {
        if (ids.isEmpty()) return List.of();

        String jpql = "SELECT cs FROM CallSession cs WHERE cs.id IN :ids ORDER BY cs.createdAt " + (direction.isAscending() ? "ASC" : "DESC");

        return em.createQuery(jpql, CallSession.class)
                .setParameter("ids", ids)
                .getResultList();
    }
}