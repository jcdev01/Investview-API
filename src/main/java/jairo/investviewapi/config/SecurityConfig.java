package jairo.investviewapi.config;

import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder(@Value("${app.jwt.secret}") String secret) {
        return NimbusJwtEncoder.withSecretKey(localSecretKey(secret)).build();
    }

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${app.oauth.google.jwk-set-uri}") String jwkSetUri,
            @Value("${app.oauth.google.issuer}") String googleIssuer,
            @Value("${app.oauth.google.client-id:}") String clientId,
            @Value("${app.jwt.secret}") String localSecret,
            @Value("${app.jwt.issuer}") String localIssuer) {
        JwtDecoder googleDecoder = googleJwtDecoder(jwkSetUri, googleIssuer, clientId);
        JwtDecoder localDecoder = localJwtDecoder(localSecret, localIssuer);
        return token -> localIssuer.equals(issuerOf(token))
                ? localDecoder.decode(token)
                : googleDecoder.decode(token);
    }

    private static JwtDecoder googleJwtDecoder(String jwkSetUri, String issuer, String clientId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(JwtValidators.createDefaultWithIssuer(issuer));
        validators.add(jwt -> {
            List<String> audience = jwt.getAudience();
            if (StringUtils.hasText(clientId) && audience != null && audience.contains(clientId)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Token não destinado a esta aplicação", null));
        });

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
    }

    private static JwtDecoder localJwtDecoder(String secret, String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(localSecretKey(secret))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    private static SecretKey localSecretKey(String secret) {
        if (!StringUtils.hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret deve ter pelo menos 32 bytes");
        }
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private static String issuerOf(String token) {
        try {
            return SignedJWT.parse(token).getJWTClaimsSet().getIssuer();
        } catch (ParseException ex) {
            throw new BadJwtException("Token JWT inválido", ex);
        }
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/h2-console/**", h2ConsoleCorsConfiguration());
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    AuthenticationManager authenticationManager(
            ObjectProvider<UserDetailsService> userDetailsServices,
            PasswordEncoder passwordEncoder) {
        List<AuthenticationProvider> providers = userDetailsServices.orderedStream()
                .map(userDetailsService -> {
                    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
                    provider.setPasswordEncoder(passwordEncoder);
                    return (AuthenticationProvider) provider;
                })
                .toList();
        return new ProviderManager(providers);
    }

    private static CorsConfiguration h2ConsoleCorsConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);
        return configuration;
    }
}
