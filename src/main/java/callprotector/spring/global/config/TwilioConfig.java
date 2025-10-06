package callprotector.spring.global.config;

import com.twilio.Twilio;
import com.twilio.http.TwilioRestClient;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "twilio")
public class TwilioConfig {
    @Value("${twilio.account.sid}")
    private String twilioAccountSid;

    @Value("${twilio.auth.token}")
    private String twilioAuthToken;

    @Value("${twilio.api.key}")
    private String twilioApiKey;

    @Value("${twilio.api.secret}")
    private String twilioApiSecret;

    @PostConstruct
    public void init() {
        Twilio.init(
                getTwilioAccountSid(),
                getTwilioAuthToken()
        );
    }

    @Bean
    public TwilioRestClient twilioRestClient() {
        return new TwilioRestClient.Builder(twilioApiKey, twilioApiSecret)
                .accountSid(twilioAccountSid)
                .build();
    }
}
