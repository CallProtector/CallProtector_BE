package callprotector.spring.domain.abuse.service;

import callprotector.spring.domain.abuse.dto.response.AbuseFilterResponseDTO;
import callprotector.spring.domain.calllog.entity.CallLog;


public interface AbuseService {

    AbuseFilterResponseDTO analyzeText(String text);

    void saveAbuseLogs(CallLog callLog, String abuseTypeStr);
}

