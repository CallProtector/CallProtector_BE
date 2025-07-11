package callprotector.spring.service.AbuseService;

import callprotector.spring.domain.CallLog;
import callprotector.spring.web.dto.response.AbuseResponseDTO;

public interface AbuseService {

    AbuseResponseDTO.AbuseFilterDTO analyzeText(String text);

    void saveAbuseLogs(CallLog callLog, String abuseTypeStr);
}

