package jairo.investviewapi.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jairo.investviewapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldProvisionAndReturnGoogleUser() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .with(jwt().jwt(jwt -> jwt
                                .subject("google-123")
                                .claim("email", "google-user@example.com")
                                .claim("email_verified", true)
                                .claim("name", "Google User"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("google-user@example.com"))
                .andExpect(jsonPath("$.name").value("Google User"));

        assertThat(userRepository.findByEmail("google-user@example.com")).isPresent();
    }

    @Test
    void shouldRejectTokenWithoutVerifiedEmail() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .with(jwt().jwt(jwt -> jwt
                                .subject("google-456")
                                .claim("email", "unverified@example.com")
                                .claim("email_verified", false))))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.findByEmail("unverified@example.com")).isEmpty();
    }
}
