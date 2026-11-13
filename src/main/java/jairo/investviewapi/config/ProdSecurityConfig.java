package jairo.investviewapi.config;

import jairo.investviewapi.security.GoogleUserSyncFilter;
import jairo.investviewapi.service.GoogleUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("prod")
@RequiredArgsConstructor
public class ProdSecurityConfig {

    private final GoogleUserService googleUserService;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers("/auth/login", "/auth/forgot-password").permitAll()
                        .requestMatchers("/users/*/favorites/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/users/*/portfolio/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/users/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .addFilterAfter(new GoogleUserSyncFilter(googleUserService), BearerTokenAuthenticationFilter.class);
        return http.build();
    }
}
