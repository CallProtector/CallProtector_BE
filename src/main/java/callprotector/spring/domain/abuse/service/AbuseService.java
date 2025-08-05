package callprotector.spring.domain.abuse.service;

import callprotector.spring.domain.abuse.dto.response.AbuseFilterResponseDTO;
import callprotector.spring.domain.calllog.entity.CallLog;
import callprotector.spring.domain.abuse.dto.response.AbuseResponseDTO;

public interface AbuseService {

    AbuseFilterResponseDTO analyzeText(String text);

    void saveAbuseLogs(CallLog callLog, String abuseTypeStr);
}

