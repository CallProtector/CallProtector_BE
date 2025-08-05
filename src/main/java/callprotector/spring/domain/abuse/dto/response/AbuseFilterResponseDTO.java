package callprotector.spring.domain.abuse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AbuseFilterResponseDTO {
    private boolean abuse;
    private boolean detected;
    private String type;
}