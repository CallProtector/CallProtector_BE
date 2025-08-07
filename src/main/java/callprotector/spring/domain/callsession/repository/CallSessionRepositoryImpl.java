package callprotector.spring.domain.callsession.repository;

import callprotector.spring.domain.callsession.entity.CallSession;
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
    public List<CallSession> findFirstPageByUserId(Long userId, String sortBy, int limit, Sort.Direction direction) {
        String jpql = "SELECT c FROM CallSession c WHERE c.user.id = :userId ORDER BY c." + sortBy + " " + direction.name();

        return em.createQuery(jpql, CallSession.class)
                .setParameter("userId", userId)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public List<CallSession> findByUserIdAndCursor(Long userId, String sortBy, Object cursorValue, int limit, Sort.Direction direction) {
        String operator = direction.isAscending() ? ">" : "<";

        String jpql = "SELECT c FROM CallSession c " +
                "WHERE c.user.id = :userId AND c." + sortBy + " " + operator + " :cursorValue " +
                "ORDER BY c." + sortBy + " " + direction.name();

        return em.createQuery(jpql, CallSession.class)
                .setParameter("userId", userId)
                .setParameter("cursorValue", cursorValue)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public List<CallSession> findSessionsByAbuseCategoryAndUserId(String category, Long userId, Long cursorId, int limit, Sort.Direction direction) {
        String operator = direction.isAscending() ? ">" : "<";

        String jpql = "SELECT DISTINCT cs FROM CallSession cs " +
                "JOIN CallLog cl ON cl.callSession = cs " +
                "JOIN AbuseLog al ON al.callLog = cl " +
                "JOIN AbuseTypeLog atl ON atl.abuseLog = al " +
                "JOIN AbuseType at ON at = atl.abuseType " +
                "WHERE cs.user.id = :userId " +
                "AND at." + category + " = true " +
                (cursorId != null ? "AND cs.id " + operator + " :cursorId " : "") +
                "ORDER BY cs.id " + direction.name();

        TypedQuery<CallSession> query = em.createQuery(jpql, CallSession.class)
                .setParameter("userId", userId)
                .setMaxResults(limit + 1);

        if (cursorId != null) {
            query.setParameter("cursorId", cursorId);
        }

        return query.getResultList();
    }

    @Override
    public List<CallSession> findByIdsWithOrderAndUserId(List<Long> ids, Sort.Direction direction, Long userId) {
        if (ids.isEmpty()) return List.of();

        String jpql = "SELECT cs FROM CallSession cs " +
                "WHERE cs.id IN :ids AND cs.user.id = :userId " +
                "ORDER BY cs.createdAt " + (direction.isAscending() ? "ASC" : "DESC");

        return em.createQuery(jpql, CallSession.class)
                .setParameter("ids", ids)
                .setParameter("userId", userId)
                .getResultList();
    }

    @Override
    public List<CallSession> findAbusiveCallSessions(Long userId, Long cursorId, int limit) {
        String jpql =
                "SELECT DISTINCT cs FROM CallSession cs " +
                        "JOIN CallLog cl ON cl.callSession = cs " +
                        "JOIN AbuseLog al ON al.callLog = cl " +
                        "WHERE cs.user.id = :userId " +
                        (cursorId != null ? "AND cs.id < :cursorId " : "") +
                        "ORDER BY cs.id DESC";

        TypedQuery<CallSession> query = em.createQuery(jpql, CallSession.class)
                .setParameter("userId", userId)
                .setMaxResults(limit);

        if (cursorId != null) {
            query.setParameter("cursorId", cursorId);
        }

        return query.getResultList();
    }

}