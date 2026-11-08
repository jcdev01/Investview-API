package jairo.investviewapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jairo.investviewapi.entity.User;
import jairo.investviewapi.service.GoogleUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class GoogleUserSyncFilter extends OncePerRequestFilter {

    public static final String CURRENT_USER_ATTRIBUTE = "currentUser";

    private final GoogleUserService googleUserService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            Jwt jwt = jwtAuthentication.getToken();
            String email = jwt.getClaimAsString("email");
            Object emailVerifiedClaim = jwt.getClaim("email_verified");
            boolean emailVerified = emailVerifiedClaim instanceof Boolean value && value;
            if (email != null && emailVerified) {
                User user = googleUserService.provision(
                        email, jwt.getClaimAsString("name"), jwt.getSubject());
                request.setAttribute(CURRENT_USER_ATTRIBUTE, user);

                List<GrantedAuthority> authorities = new ArrayList<>(jwtAuthentication.getAuthorities());
                authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

                JwtAuthenticationToken updatedAuth = new JwtAuthenticationToken(
                        jwt, authorities, jwtAuthentication.getName());
                SecurityContextHolder.getContext().setAuthentication(updatedAuth);
            }
        }
        filterChain.doFilter(request, response);
    }
}
