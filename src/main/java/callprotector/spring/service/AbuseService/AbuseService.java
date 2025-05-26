package callprotector.spring.service.AbuseService;

import callprotector.spring.web.dto.response.AbuseResponseDTO;

public interface AbuseService {

    public AbuseResponseDTO.AbuseFilterDTO analyzeText(String text);

}
