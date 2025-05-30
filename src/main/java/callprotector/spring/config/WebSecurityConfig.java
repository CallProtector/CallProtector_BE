package callprotector.spring.config;

import callprotector.spring.apiPayload.ApiResponse;
import callprotector.spring.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.CorsFilter;

import java.io.PrintWriter;

@Configuration
@EnableWebSecurity
@Slf4j
public class WebSecurityConfig
{

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) {
        try {

            http.csrf(AbstractHttpConfigurer::disable)
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .cors(Customizer.withDefaults())
                    .sessionManagement((sessionManagement) ->
                            sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    )

                    .authorizeHttpRequests((authorizeRequests) ->
                            authorizeRequests
                                    .requestMatchers(
                                            "/health",
                                            "/users/auth/**",
                                            "/school/**",
                                            "/v3/api-docs/**",
                                            "/swagger-ui/**",
                                            "/swagger-resources/**",
                                            "/chat/**",
                                            "/mail/**",
                                            "/api/webhook/**",
                                            "/twiml",
                                            "/ws/**",
                                            "/token/**",
                                            "/twilio/voice",
                                            "/popup.html",
                                            "/call-session/**",
                                            "/abuse/**")
                                    .permitAll()
                                    .anyRequest().authenticated()
                    )
                    .exceptionHandling((exceptionConfig) ->
                            exceptionConfig
                                    .authenticationEntryPoint(unauthorizedEntryPoint)
                    ); // 401 403 관련 예외처리

            http.addFilterAfter(
                    jwtAuthenticationFilter,
                    CorsFilter.class
            );
            return http.build();
        } catch (Exception e) {

            throw new RuntimeException(e);
        }

    }

    private final AuthenticationEntryPoint unauthorizedEntryPoint =
            (request, response, authException) -> {

                ApiResponse<?> apiResponse = new ApiResponse(false,"401","인증이 필요합니다.",null);
                response.setCharacterEncoding("UTF-8");
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                PrintWriter writer = response.getWriter();
                writer.write(new ObjectMapper().writeValueAsString(apiResponse));
                writer.flush();
            };

}
