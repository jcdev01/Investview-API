package jairo.investviewapi.service;

import jairo.investviewapi.entity.Role;
import jairo.investviewapi.entity.User;
import jairo.investviewapi.repository.UserRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class GoogleUserService {

    private final UserRepository userRepository;
    private final Set<String> adminEmails;

    public GoogleUserService(
            UserRepository userRepository,
            @Value("${app.security.admin-emails:}") String adminEmailsProperty) {
        this.userRepository = userRepository;
        this.adminEmails = parseAdminEmails(adminEmailsProperty);
    }

    @Transactional
    public User provision(String email, String name, String googleSub) {
        Role targetRole = adminEmails.contains(email.toLowerCase()) ? Role.ADMIN : Role.USER;

        User user = userRepository.findByEmail(email)
                .map(existing -> {
                    if (existing.getRole() != targetRole && targetRole == Role.ADMIN) {
                        existing.setRole(Role.ADMIN);
                    }
                    return existing;
                })
                .orElseGet(() -> User.builder()
                        .name(name != null && !name.isBlank() ? name : email)
                        .email(email)
                        .role(targetRole)
                        .build());

        if (user.getGoogleSub() == null && googleSub != null) {
            user.setGoogleSub(googleSub);
        }
        return userRepository.save(user);
    }

    private static Set<String> parseAdminEmails(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptySet();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }
}
