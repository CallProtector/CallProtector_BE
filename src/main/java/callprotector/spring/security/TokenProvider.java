package callprotector.spring.security;

import callprotector.spring.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;

@Slf4j
@Service
public class TokenProvider {
    // 64바이트(512비트) 이상의 고정된 키 사용 (실제 운영에선 환경변수로 관리)
    private static final String SECRET_STRING = "Zb1kG!2p7sX9qLm4Vw8eR6tYc3uJ0hN5bQzXvLkMnBvCjTgRzLpQwErTyUiOpAsDfGhJkLzXcVbN";
    private final SecretKey key = Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));

    public String create(User user) {
        Date expiryDate = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
        return Jwts.builder()
                .signWith(key, SignatureAlgorithm.HS512) // signWith 파라미터 순서 변경
                .setSubject(user.getEmail())
                .setIssuer("callprotector web")
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .compact();
    }

    public String validateAndGetUserEmail(String token) {
        Claims claims = Jwts.parserBuilder() // parser() → parserBuilder()로 변경
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
}
