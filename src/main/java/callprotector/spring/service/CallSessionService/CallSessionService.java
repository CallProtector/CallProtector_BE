package callprotector.spring.service.CallSessionService;

import callprotector.spring.web.dto.request.CallSessionRequestDTO;

public interface CallSessionService {
    Long createCallSession(String email, CallSessionRequestDTO.CallSessionMakeDTO dto );
}
