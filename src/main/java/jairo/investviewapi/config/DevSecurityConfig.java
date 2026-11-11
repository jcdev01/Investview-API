package jairo.investviewapi.config;


import jairo.investviewapi.security.GoogleUserSyncFilter;
import jairo.investviewapi.service.GoogleUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;


@Configuration

@Profile("!prod")

@RequiredArgsConstructor

public class DevSecurityConfig {


    private final GoogleUserService googleUserService;


    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http

                .cors(Customizer.withDefaults())

                .csrf(csrf -> csrf.disable())

                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(

                                "/swagger-ui.html",

                                "/swagger-ui/**",

                                "/v3/api-docs/**",

                                "/h2-console/**")

                        .permitAll()

                        .requestMatchers("/users/*/favorites/**").hasAnyRole("USER", "ADMIN")

                        .requestMatchers("/users/**").hasRole("ADMIN")

                        .anyRequest().authenticated())

                .formLogin(Customizer.withDefaults())

                .httpBasic(Customizer.withDefaults())

                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))

                .addFilterAfter(new GoogleUserSyncFilter(googleUserService), BearerTokenAuthenticationFilter.class);

        return http.build();

    }


    @Bean
    UserDetailsService userDetailsService(

            @Value("${app.security.username}") String username,

            @Value("${app.security.password}") String password,

            PasswordEncoder passwordEncoder) {

        UserDetails user = User.withUsername(username)

                .password(passwordEncoder.encode(password))

                .roles("ADMIN", "USER")

                .build();

        return new InMemoryUserDetailsManager(user);

    }

}