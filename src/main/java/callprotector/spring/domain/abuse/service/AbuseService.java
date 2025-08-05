package callprotector.spring.domain.abuse.service;

import callprotector.spring.domain.abuse.dto.response.AbuseResponseDTO;
import callprotector.spring.domain.calllog.entity.CallLog;

public interface AbuseService {

    AbuseResponseDTO.AbuseFilterDTO analyzeText(String text);

    void saveAbuseLogs(CallLog callLog, String abuseTypeStr);
}

