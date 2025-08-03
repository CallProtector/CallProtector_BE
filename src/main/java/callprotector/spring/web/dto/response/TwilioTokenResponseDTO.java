package callprotector.spring.web.dto.response;

public record TwilioTokenResponseDTO(
        String twilioAccessToken,
        Long userId
) {

    public static TwilioTokenResponseDTO of(String token, Long userId) {

        return new TwilioTokenResponseDTO(token, userId);
    }
}
