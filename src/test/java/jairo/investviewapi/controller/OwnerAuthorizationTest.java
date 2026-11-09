package jairo.investviewapi.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jairo.investviewapi.entity.Role;
import jairo.investviewapi.entity.User;
import jairo.investviewapi.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OwnerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private User other;
    private User admin;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder()
                .name("Owner")
                .email("owner-" + UUID.randomUUID() + "@example.com")
                .role(Role.USER)
                .build());
        other = userRepository.save(User.builder()
                .name("Other")
                .email("other-" + UUID.randomUUID() + "@example.com")
                .role(Role.USER)
                .build());
        admin = userRepository.save(User.builder()
                .name("Admin")
                .email("admin-" + UUID.randomUUID() + "@example.com")
                .role(Role.ADMIN)
                .build());
    }

    @Test
    void shouldDenyNormalUserFromDeletingAnyUser() throws Exception {
        mockMvc.perform(delete("/users/{id}", other.getId()).with(jwtFor(owner)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/users/{id}", owner.getId()).with(jwtFor(owner)))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findById(other.getId())).isPresent();
        assertThat(userRepository.findById(owner.getId())).isPresent();
    }

    @Test
    void shouldDenyNormalUserFromAccessingUserManagementRoutes() throws Exception {
        mockMvc.perform(get("/users/{id}", owner.getId()).with(jwtFor(owner)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/users").with(jwtFor(owner)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/users")
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacker\",\"email\":\"hacker@example.com\",\"password\":\"123456\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/users/{id}", other.getId())
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacked\",\"email\":\"hacked@example.com\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminToManageAndBatchDeleteUsers() throws Exception {
        mockMvc.perform(get("/users").with(jwtFor(admin)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/{id}", other.getId()).with(jwtFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(other.getEmail()));

        mockMvc.perform(delete("/users/{id}", other.getId()).with(jwtFor(admin)))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(other.getId())).isEmpty();
    }

    @Test
    void shouldAllowAccessToOwnFavorites() throws Exception {
        mockMvc.perform(get("/users/{id}/favorites", owner.getId()).with(jwtFor(owner)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenyAccessToAnotherUserFavorites() throws Exception {
        mockMvc.perform(get("/users/{id}/favorites", other.getId()).with(jwtFor(owner)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminToAccessAnotherUserFavorites() throws Exception {
        mockMvc.perform(get("/users/{id}/favorites", other.getId()).with(jwtFor(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenyAccessWhenEmailIsNotVerified() throws Exception {
        mockMvc.perform(get("/users/{id}/favorites", owner.getId())
                        .with(jwt().jwt(jwt -> jwt
                                .subject("google-unverified")
                                .claim("email", owner.getEmail())
                                .claim("email_verified", false))))
                .andExpect(status().isForbidden());
    }

    private static JwtRequestPostProcessor jwtFor(User user) {
        return jwt().jwt(jwt -> jwt
                .subject("google-" + user.getId())
                .claim("email", user.getEmail())
                .claim("email_verified", true)
                .claim("name", user.getName()));
    }
}
