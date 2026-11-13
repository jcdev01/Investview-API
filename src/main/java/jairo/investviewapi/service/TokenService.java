package jairo.investviewapi.service;

import jairo.investviewapi.dto.TokenResponse;
import jairo.investviewapi.entity.User;
import jairo.investviewapi.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final UserRepository userRepository;
    private final String issuer;
    private final Duration ttl;

    public TokenService(
            JwtEncoder jwtEncoder,
            UserRepository userRepository,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.ttl}") Duration ttl) {
        this.jwtEncoder = jwtEncoder;
        this.userRepository = userRepository;
        this.issuer = issuer;
        this.ttl = ttl;
    }

    @Transactional(readOnly = true)
    public TokenResponse generate(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException(
                        "Login local disponível apenas para usuários cadastrados."));

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim("email", user.getEmail())
                .claim("email_verified", true)
                .claim("name", user.getName())
                .claim("role", user.getRole().name())
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", ttl.toSeconds());
    }
}
