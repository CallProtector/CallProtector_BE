package callprotector.spring.service.AbuseService;


import callprotector.spring.client.FastClient;
import callprotector.spring.web.dto.response.AbuseResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AbuseServiceImpl implements AbuseService{


    private final FastClient fastClient;

    @Override
    public AbuseResponseDTO.AbuseFilterDTO analyzeText(String text) {
        return fastClient.sendTextToFlask(text);
    }
}
