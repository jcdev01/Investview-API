package jairo.investviewapi.graph;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jairo.investviewapi.entity.Favorite;
import jairo.investviewapi.entity.Role;
import jairo.investviewapi.entity.User;
import jairo.investviewapi.repository.FavoriteRepository;
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
class GraphControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    private User requester;

    @BeforeEach
    void setUp() {
        favoriteRepository.deleteAll();
        requester = userRepository.save(User.builder()
                .name("Requester")
                .email("graph-" + UUID.randomUUID() + "@example.com")
                .role(Role.USER)
                .build());
    }

    @Test
    void shouldReturnEmptyGraphWhenThereAreNoFavorites() throws Exception {
        mockMvc.perform(get("/graph").with(jwtFor(requester)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes").isEmpty())
                .andExpect(jsonPath("$.edges").isEmpty());

        mockMvc.perform(get("/graph/components").with(jwtFor(requester)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldBuildGraphWithNodesAndWeightedEdges() throws Exception {
        User first = newUser("first");
        User second = newUser("second");
        favorite(first, "PETR4", "Petrobras");
        favorite(first, "VALE3", "Vale");
        favorite(first, "BTC", "Bitcoin");
        favorite(second, "PETR4", null);
        favorite(second, "VALE3", null);

        mockMvc.perform(get("/graph").with(jwtFor(requester)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes.length()").value(3))
                .andExpect(jsonPath("$.nodes[?(@.id == 'PETR4')].name").value(hasItem("Petrobras")))
                .andExpect(jsonPath("$.nodes[?(@.id == 'VALE3')].name").value(hasItem("Vale")))
                .andExpect(jsonPath("$.nodes[?(@.id == 'BTC')].name").value(hasItem("Bitcoin")))
                .andExpect(jsonPath("$.edges.length()").value(3))
                .andExpect(jsonPath("$.edges[?(@.source == 'PETR4' && @.target == 'VALE3')].weight")
                        .value(hasItem(2)))
                .andExpect(jsonPath("$.edges[?(@.source == 'BTC' && @.target == 'PETR4')].weight")
                        .value(hasItem(1)))
                .andExpect(jsonPath("$.edges[?(@.source == 'BTC' && @.target == 'VALE3')].weight")
                        .value(hasItem(1)));
    }

    @Test
    void shouldReturnIsolatedAssetAsNodeWithoutEdges() throws Exception {
        User first = newUser("first");
        favorite(first, "PETR4", "Petrobras");

        mockMvc.perform(get("/graph").with(jwtFor(requester)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes.length()").value(1))
                .andExpect(jsonPath("$.nodes[0].id").value("PETR4"))
                .andExpect(jsonPath("$.edges").isEmpty());

        mockMvc.perform(get("/graph/assets/PETR4/neighbors").with(jwtFor(requester)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturnNeighborsSortedByWeightDesc() throws Exception {
        User first = newUser("first");
        User second = newUser("second");
        User third = newUser("third");
        favorite(first, "VALE3", null);
        favorite(first, "PETR4", null);
        favorite(second, "VALE3", null);
        favorite(second, "PETR4", null);
        favorite(second, "BTC", null);
        favorite(third, "VALE3", null);
        favorite(third, "ITSA4", null);

        mockMvc.perform(get("/graph/assets/VALE3/neighbors").with(jwtFor(requester)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value("PETR4"))
                .andExpect(jsonPath("$[0].weight").value(2))
                .andExpect(jsonPath("$[1].id").value("BTC"))
                .andExpect(jsonPath("$[1].weight").value(1))
                .andExpect(jsonPath("$[2].id").value("ITSA4"))
                .andExpect(jsonPath("$[2].weight").value(1));
    }

    @Test
    void shouldReturnShortestPathBetweenAssets() throws Exception {
        User first = newUser("first");
        User second = newUser("second");
        favorite(first, "PETR4", null);
        favorite(first, "VALE3", null);
        favorite(second, "VALE3", null);
        favorite(second, "BTC", null);

        mockMvc.perform(get("/graph/path")
                        .queryParam("from", "PETR4")
                        .queryParam("to", "BTC")
                        .with(jwtFor(requester)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.path.length()").value(3))
                .andExpect(jsonPath("$.path[0]").value("PETR4"))
                .andExpect(jsonPath("$.path[1]").value("VALE3"))
                .andExpect(jsonPath("$.path[2]").value("BTC"))
                .andExpect(jsonPath("$.length").value(2));
    }

    @Test
    void shouldReturnEmptyPathWhenAssetsAreDisconnected() throws Exception {
        User first = newUser("first");
        User second = newUser("second");
        favorite(first, "PETR4", null);
        favorite(first, "VALE3", null);
        favorite(second, "BTC", null);

        mockMvc.perform(get("/graph/path")
                        .queryParam("from", "PETR4")
                        .queryParam("to", "BTC")
                        .with(jwtFor(requester)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(false))
                .andExpect(jsonPath("$.path").isEmpty())
                .andExpect(jsonPath("$.length").value(0));
    }

    @Test
    void shouldReturnNotFoundWhenAssetIsNotInTheGraph() throws Exception {
        mockMvc.perform(get("/graph/assets/ITUB4/neighbors").with(jwtFor(requester)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/graph/path")
                        .queryParam("from", "PETR4")
                        .queryParam("to", "BTC")
                        .with(jwtFor(requester)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequestWhenAssetSymbolIsBlank() throws Exception {
        mockMvc.perform(get("/graph/path")
                        .queryParam("from", "")
                        .queryParam("to", "PETR4")
                        .with(jwtFor(requester)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnConnectedComponents() throws Exception {
        User first = newUser("first");
        User second = newUser("second");
        User third = newUser("third");
        favorite(first, "PETR4", null);
        favorite(first, "VALE3", null);
        favorite(second, "BTC", null);
        favorite(second, "ETH", null);
        favorite(third, "ITSA4", null);

        mockMvc.perform(get("/graph/components").with(jwtFor(requester)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].length()").value(2))
                .andExpect(jsonPath("$[0][0]").value("BTC"))
                .andExpect(jsonPath("$[0][1]").value("ETH"))
                .andExpect(jsonPath("$[1][0]").value("PETR4"))
                .andExpect(jsonPath("$[1][1]").value("VALE3"))
                .andExpect(jsonPath("$[2].length()").value(1))
                .andExpect(jsonPath("$[2][0]").value("ITSA4"));
    }

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/graph").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    private User newUser(String name) {
        return userRepository.save(User.builder()
                .name(name)
                .email(name + "-" + UUID.randomUUID() + "@example.com")
                .role(Role.USER)
                .build());
    }

    private void favorite(User user, String symbol, String name) {
        favoriteRepository.save(Favorite.builder()
                .user(user)
                .symbol(symbol)
                .name(name)
                .build());
    }

    private static JwtRequestPostProcessor jwtFor(User user) {
        return jwt().jwt(jwt -> jwt
                .subject("graph-" + user.getId())
                .claim("email", user.getEmail())
                .claim("email_verified", true)
                .claim("name", user.getName()));
    }
}
