package jairo.investviewapi.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jairo.investviewapi.entity.Role;
import jairo.investviewapi.entity.User;
import jairo.investviewapi.repository.PortfolioItemRepository;
import jairo.investviewapi.repository.PortfolioRepository;
import jairo.investviewapi.repository.UserRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class PortfolioControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private PortfolioItemRepository portfolioItemRepository;

    private User owner;
    private User other;
    private User admin;

    @BeforeEach
    void setUp() {
        portfolioItemRepository.deleteAll();
        portfolioRepository.deleteAll();
        owner = newUser("Owner", Role.USER);
        other = newUser("Other", Role.USER);
        admin = newUser("Admin", Role.ADMIN);
    }

    @Test
    void shouldCreateEmptyPortfolioOnFirstAccess() throws Exception {
        mockMvc.perform(get("/users/{id}/portfolio", owner.getId()).with(jwtFor(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Minha carteira"))
                .andExpect(jsonPath("$.items").isEmpty());

        assertThat(portfolioRepository.findByUserId(owner.getId())).isPresent();
    }

    @Test
    void shouldAddItemNormalizingSymbolAndName() throws Exception {
        MvcResult result = addItem(owner, " petr4 ", " Petrobras ", "10")
                .andExpect(jsonPath("$.symbol").value("PETR4"))
                .andExpect(jsonPath("$.name").value("Petrobras"))
                .andReturn();

        assertQuantity(result, "10");
    }

    @Test
    void shouldIncrementQuantityWhenAddingSameAsset() throws Exception {
        addItem(owner, "PETR4", "Petrobras", "10");

        MvcResult second = addItem(owner, "petr4", null, "5").andReturn();
        assertQuantity(second, "15");

        mockMvc.perform(get("/users/{id}/portfolio/items", owner.getId()).with(jwtFor(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].symbol").value("PETR4"));
    }

    @Test
    void shouldReturnPortfolioWithItems() throws Exception {
        addItem(owner, "VALE3", "Vale", "3");

        mockMvc.perform(get("/users/{id}/portfolio", owner.getId()).with(jwtFor(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].symbol").value("VALE3"));
    }

    @Test
    void shouldUpdateItemQuantityAndName() throws Exception {
        MvcResult added = addItem(owner, "PETR4", "Petrobras", "10").andReturn();
        long itemId = itemId(added);

        MvcResult updated = mockMvc.perform(put("/users/{userId}/portfolio/items/{itemId}", owner.getId(), itemId)
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3,\"name\":\"Petrobras PN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Petrobras PN"))
                .andReturn();

        assertQuantity(updated, "3");
    }

    @Test
    void shouldRemoveItem() throws Exception {
        MvcResult added = addItem(owner, "PETR4", "Petrobras", "10").andReturn();
        long itemId = itemId(added);

        mockMvc.perform(delete("/users/{userId}/portfolio/items/{itemId}", owner.getId(), itemId)
                        .with(jwtFor(owner)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/{id}/portfolio/items", owner.getId()).with(jwtFor(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldRenamePortfolio() throws Exception {
        mockMvc.perform(put("/users/{id}/portfolio", owner.getId())
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Carteira Dividendos\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Carteira Dividendos"));
    }

    @Test
    void shouldDenyAccessToAnotherUserPortfolio() throws Exception {
        mockMvc.perform(get("/users/{id}/portfolio", other.getId()).with(jwtFor(owner)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminToAccessAnotherUserPortfolio() throws Exception {
        mockMvc.perform(get("/users/{id}/portfolio", owner.getId()).with(jwtFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Minha carteira"));
    }

    @Test
    void shouldRejectNonPositiveQuantity() throws Exception {
        mockMvc.perform(post("/users/{id}/portfolio/items", owner.getId())
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"PETR4\",\"quantity\":0}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/users/{id}/portfolio/items", owner.getId())
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"PETR4\",\"quantity\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        mockMvc.perform(get("/users/{id}/portfolio", 999_999L).with(jwtFor(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/users/{id}/portfolio", owner.getId()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions addItem(
            User user, String symbol, String name, String quantity) throws Exception {
        String body = name == null
                ? "{\"symbol\":\"%s\",\"quantity\":%s}".formatted(symbol, quantity)
                : "{\"symbol\":\"%s\",\"name\":\"%s\",\"quantity\":%s}"
                        .formatted(symbol, name, quantity);
        return mockMvc.perform(post("/users/{id}/portfolio/items", user.getId())
                        .with(jwtFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private static long itemId(MvcResult result) throws Exception {
        JsonNode node = OBJECT_MAPPER.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private static void assertQuantity(MvcResult result, String expected) throws Exception {
        JsonNode node = OBJECT_MAPPER.readTree(result.getResponse().getContentAsString());
        assertThat(node.get("quantity").decimalValue())
                .isEqualByComparingTo(new BigDecimal(expected));
    }

    private User newUser(String name, Role role) {
        return userRepository.save(User.builder()
                .name(name)
                .email(name.toLowerCase() + "-" + UUID.randomUUID() + "@example.com")
                .role(role)
                .build());
    }

    private static JwtRequestPostProcessor jwtFor(User user) {
        return jwt().jwt(jwt -> jwt
                .subject("portfolio-" + user.getId())
                .claim("email", user.getEmail())
                .claim("email_verified", true)
                .claim("name", user.getName()));
    }
}
