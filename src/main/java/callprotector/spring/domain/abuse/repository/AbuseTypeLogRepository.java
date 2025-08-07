package callprotector.spring.domain.abuse.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import callprotector.spring.domain.abuse.entity.AbuseType;
import callprotector.spring.domain.abuse.entity.AbuseTypeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AbuseTypeLogRepository extends JpaRepository<AbuseTypeLog, Long> {
    @Query("SELECT atl.abuseType FROM AbuseTypeLog atl " +
            "JOIN atl.abuseLog al " +
            "JOIN al.callLog cl " +
            "WHERE cl.callSession.id = :sessionId")
    List<AbuseType> findAbuseTypesByCallSessionId(@Param("sessionId") Long sessionId);

    boolean existsByAbuseLog_CallLog_CallSession_IdAndAbuseType_VerbalAbuseTrue(Long sessionId);

    boolean existsByAbuseLog_CallLog_CallSession_IdAndAbuseType_SexualHarassTrue(Long sessionId);

    boolean existsByAbuseLog_CallLog_CallSession_IdAndAbuseType_ThreatTrue(Long sessionId);
}