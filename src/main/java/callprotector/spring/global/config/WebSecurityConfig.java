package callprotector.spring.global.config;

import callprotector.spring.global.apiPayload.ApiResponse;
import callprotector.spring.global.security.JwtAuthenticationFilter;
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
                                            "/v3/api-docs/**",
                                            "/swagger-ui/**",
                                            "/swagger-resources/**",
                                            "/api/auth/**",
                                            "/twilio/voice",
                                            "/popup.html",
                                            "/api/abuse/**",
                                            "/ws/**", // 인증 인가 처리 후 삭제 예정
                                            "/chatstream.html",  // [Chat 관련] 임시 프론트 테스트용 HTML 직접 접근 허용
                                            "/api/chat/stream",
                                            "/chatsessionstream.html", // [상담별 Chat 관련] 임시 프론트 테스트용 HTML 직접 접근 허용
                                            "/api/chatbot/analyze/**",
                                            "/callchatstream.html", // [상담별 Chat 후속 관련] 임시 프론트 테스트용 HTML 직접 접근 허용
                                            "/api/call-chat/stream"
                                    )
                                    .permitAll()
                                    .anyRequest().authenticated()
                    )
                    // ★ 인증/인가 실패 처리
                    .exceptionHandling(exception -> exception
                            // 인증 실패 (JWT 없거나 잘못된 경우 → 401)
                            .authenticationEntryPoint(unauthorizedEntryPoint)
                            // 인가 실패 (권한 부족 → 403)
                            .accessDeniedHandler((request, response, accessDeniedException) -> {
                                ApiResponse<?> apiResponse = new ApiResponse<>(false, "403", "접근 권한이 없습니다.", null);
                                response.setCharacterEncoding("UTF-8");
                                response.setStatus(HttpStatus.FORBIDDEN.value());
                                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                PrintWriter writer = response.getWriter();
                                writer.write(new ObjectMapper().writeValueAsString(apiResponse));
                                writer.flush();
                            })
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

    // 인증되지 않은 요청에 대한 EntryPoint 처리
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
