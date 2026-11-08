package jairo.investviewapi.security;

import jairo.investviewapi.entity.User;
import jairo.investviewapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("currentUserProvider")
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Usuário não autenticado");
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            Jwt jwt = jwtAuthentication.getToken();
            String email = jwt.getClaimAsString("email");
            Object emailVerifiedClaim = jwt.getClaim("email_verified");
            boolean emailVerified = emailVerifiedClaim instanceof Boolean value && value;
            if (email != null && !email.isBlank() && emailVerified) {
                return userRepository.findByEmail(email)
                        .orElseThrow(() -> new AccessDeniedException("Usuário autenticado não encontrado"));
            }
        }

        throw new AccessDeniedException("Não foi possível identificar o usuário autenticado");
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    public void requireAdmin() {
        if (!isAdmin()) {
            throw new AccessDeniedException("Acesso negado: operação restrita a administradores");
        }
    }

    public void requireOwner(Long userId) {
        User currentUser = getCurrentUser();
        if (!currentUser.getId().equals(userId)) {
            throw new AccessDeniedException("Acesso negado: você só pode acessar seus próprios dados");
        }
    }

    public void requireOwnerOrAdmin(Long userId) {
        if (isAdmin()) {
            return;
        }
        requireOwner(userId);
    }

    public boolean isOwner(Long userId) {
        try {
            User currentUser = getCurrentUser();
            return currentUser.getId().equals(userId);
        } catch (Exception e) {
            return false;
        }
    }
}
