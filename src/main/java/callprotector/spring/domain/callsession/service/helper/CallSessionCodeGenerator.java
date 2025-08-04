package callprotector.spring.domain.callsession.service.helper;

import callprotector.spring.domain.callsession.repository.CallSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class CallSessionCodeGenerator {
    private final CallSessionRepository callSessionRepository;

    public String generateTodayCallSessionCode() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = today + "-";

        // 오늘 생성된 세션 수를 기준으로 +1
        long count = callSessionRepository.countByCallSessionCodeStartingWith(prefix);
        String sequence = String.format("%04d", count + 1); // 4자리 0-padding
        return prefix + sequence;
    }
}
