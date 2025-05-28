package callprotector.spring.web.dto.response;

public record TwilioTokenResponseDTO(
        String twilioAccessToken
) {

    public static TwilioTokenResponseDTO of(String token) {
        return new TwilioTokenResponseDTO(token);
    }
}
