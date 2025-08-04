package callprotector.spring.domain.abuse.service;

import callprotector.spring.domain.CallLog;
import callprotector.spring.domain.abuse.dto.response.AbuseResponseDTO;

public interface AbuseService {

    AbuseResponseDTO.AbuseFilterDTO analyzeText(String text);

    void saveAbuseLogs(CallLog callLog, String abuseTypeStr);
}

