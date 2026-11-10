package jairo.investviewapi.graph;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jairo.investviewapi.entity.AssetType;
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
        favorite(first, AssetType.STOCK, "PETR4", "Petrobras");
        favorite(first, AssetType.STOCK, "VALE3", "Vale");
        favorite(first, AssetType.CRYPTO, "BTC", "Bitcoin");
        favorite(second, AssetType.STOCK, "PETR4", null);
        favorite(second, AssetType.STOCK, "VALE3", null);

        mockMvc.perform(get("/graph").with(jwtFor(requester)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes.length()").value(3))
                .andExpect(jsonPath("$.nodes[?(@.id == 'STOCK:PETR4')].name").value(hasItem("Petrobras")))
                .andExpect(jsonPath("$.nodes[?(@.id == 'STOCK:VALE3')].name").value(hasItem("Vale")))
                .andExpect(jsonPath("$.nodes[?(@.id == 'CRYPTO:BTC')].name").value(hasItem("Bitcoin")))
                .andExpect(jsonPath("$.edges.length()").value(3))
                .andExpect(jsonPath("$.edges[?(@.source == 'STOCK:PETR4' && @.target == 'STOCK:VALE3')].weight")
                        .value(hasItem(2)))
                .andExpect(jsonPath("$.edges[?(@.source == 'CRYPTO:BTC' && @.target == 'STOCK:PETR4')].weight")
                        .value(hasItem(1)))
                .andExpect(jsonPath("$.edges[?(@.source == 'CRYPTO:BTC' && @.target == 'STOCK:VALE3')].weight")
                        .value(hasItem(1)));
    }

    @Test
    void shouldReturnIsolatedAssetAsNodeWithoutEdges() throws Exception {
        User first = newUser("first");
        favorite(first, AssetType.STOCK, "PETR4", "Petrobras");

        mockMvc.perform(get("/graph").with(jwtFor(requester)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes.length()").value(1))
                .andExpect(jsonPath("$.nodes[0].id").value("STOCK:PETR4"))
                .andExpect(jsonPath("$.edges").isEmpty());

        mockMvc.perform(get("/graph/assets/STOCK/PETR4/neighbors").with(jwtFor(requester)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturnNeighborsSortedByWeightDesc() throws Exception {
        User first = newUser("first");
        User second = newUser("second");
        User third = newUser("third");
        favorite(first, AssetType.STOCK, "VALE3", null);
        favorite(first, AssetType.STOCK, "PETR4", null);
        favorite(second, AssetType.STOCK, "VALE3", null);
        favorite(second, AssetType.STOCK, "PETR4", null);
        favorite(second, AssetType.CRYPTO, "BTC", null);
        favorite(third, AssetType.STOCK, "VALE3", null);
        favorite(third, AssetType.STOCK, "ITSA4", null);

        mockMvc.perform(get("/graph/assets/STOCK/VALE3/neighbors").with(jwtFor(requester)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value("STOCK:PETR4"))
                .andExpect(jsonPath("$[0].weight").value(2))
                .andExpect(jsonPath("$[1].id").value("CRYPTO:BTC"))
                .andExpect(jsonPath("$[1].weight").value(1))
                .andExpect(jsonPath("$[2].id").value("STOCK:ITSA4"))
                .andExpect(jsonPath("$[2].weight").value(1));
    }

    @Test
    void shouldReturnShortestPathBetweenAssets() throws Exception {
        User first = newUser("first");
        User second = newUser("second");
        favorite(first, AssetType.STOCK, "PETR4", null);
        favorite(first, AssetType.STOCK, "VALE3", null);
        favorite(second, AssetType.STOCK, "VALE3", null);
        favorite(second, AssetType.CRYPTO, "BTC", null);

        mockMvc.perform(get("/graph/path")
                        .queryParam("from", "STOCK:PETR4")
                        .queryParam("to", "CRYPTO:BTC")
                        .with(jwtFor(requester)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.path.length()").value(3))
                .andExpect(jsonPath("$.path[0]").value("STOCK:PETR4"))
                .andExpect(jsonPath("$.path[1]").value("STOCK:VALE3"))
                .andExpect(jsonPath("$.path[2]").value("CRYPTO:BTC"))
                .andExpect(jsonPath("$.length").value(2));
    }

    @Test
    void shouldReturnEmptyPathWhenAssetsAreDisconnected() throws Exception {
        User first = newUser("first");
        User second = newUser("second");
        favorite(first, AssetType.STOCK, "PETR4", null);
        favorite(first, AssetType.STOCK, "VALE3", null);
        favorite(second, AssetType.CRYPTO, "BTC", null);

        mockMvc.perform(get("/graph/path")
                        .queryParam("from", "STOCK:PETR4")
                        .queryParam("to", "CRYPTO:BTC")
                        .with(jwtFor(requester)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(false))
                .andExpect(jsonPath("$.path").isEmpty())
                .andExpect(jsonPath("$.length").value(0));
    }

    @Test
    void shouldReturnNotFoundWhenAssetIsNotInTheGraph() throws Exception {
        mockMvc.perform(get("/graph/assets/STOCK/ITUB4/neighbors").with(jwtFor(requester)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/graph/path")
                        .queryParam("from", "STOCK:PETR4")
                        .queryParam("to", "CRYPTO:BTC")
                        .with(jwtFor(requester)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequestWhenAssetTypeOrIdIsInvalid() throws Exception {
        mockMvc.perform(get("/graph/assets/INVALID/PETR4/neighbors").with(jwtFor(requester)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/graph/path")
                        .queryParam("from", "INVALID")
                        .queryParam("to", "STOCK:PETR4")
                        .with(jwtFor(requester)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/graph/path")
                        .queryParam("from", "FOO:PETR4")
                        .queryParam("to", "STOCK:PETR4")
                        .with(jwtFor(requester)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnConnectedComponents() throws Exception {
        User first = newUser("first");
        User second = newUser("second");
        User third = newUser("third");
        favorite(first, AssetType.STOCK, "PETR4", null);
        favorite(first, AssetType.STOCK, "VALE3", null);
        favorite(second, AssetType.CRYPTO, "BTC", null);
        favorite(second, AssetType.CRYPTO, "ETH", null);
        favorite(third, AssetType.STOCK, "ITSA4", null);

        mockMvc.perform(get("/graph/components").with(jwtFor(requester)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].length()").value(2))
                .andExpect(jsonPath("$[0][0]").value("CRYPTO:BTC"))
                .andExpect(jsonPath("$[0][1]").value("CRYPTO:ETH"))
                .andExpect(jsonPath("$[1][0]").value("STOCK:PETR4"))
                .andExpect(jsonPath("$[1][1]").value("STOCK:VALE3"))
                .andExpect(jsonPath("$[2].length()").value(1))
                .andExpect(jsonPath("$[2][0]").value("STOCK:ITSA4"));
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

    private void favorite(User user, AssetType type, String symbol, String name) {
        favoriteRepository.save(Favorite.builder()
                .user(user)
                .type(type)
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
