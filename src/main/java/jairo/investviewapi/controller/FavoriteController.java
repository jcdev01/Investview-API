package jairo.investviewapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jairo.investviewapi.dto.FavoriteRequest;
import jairo.investviewapi.dto.FavoriteResponse;
import jairo.investviewapi.security.CurrentUserProvider;
import jairo.investviewapi.service.FavoriteService;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Favoritos", description = "Endpoints para gerenciamento de ativos favoritos dos usuários")
@RestController
@RequestMapping("/users/{userId}/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final CurrentUserProvider currentUserProvider;

    @Operation(summary = "Adiciona um ativo aos favoritos do usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ativo favoritado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado aos favoritos de outro usuário"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "409", description = "Ativo já está nos favoritos do usuário")
    })
    @PostMapping
    public ResponseEntity<FavoriteResponse> add(
            @PathVariable Long userId,
            @Valid @RequestBody FavoriteRequest request) {
        currentUserProvider.requireOwner(userId);
        FavoriteResponse response = favoriteService.add(userId, request);
        return ResponseEntity
                .created(URI.create("/users/" + userId + "/favorites/" + response.id()))
                .body(response);
    }

    @Operation(summary = "Lista todos os ativos favoritos de um usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de favoritos retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado aos favoritos de outro usuário"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping
    public ResponseEntity<List<FavoriteResponse>> listByUser(@PathVariable Long userId) {
        currentUserProvider.requireOwner(userId);
        return ResponseEntity.ok(favoriteService.listByUser(userId));
    }

    @Operation(summary = "Remove um ativo dos favoritos do usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Favorito removido com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado aos favoritos de outro usuário"),
            @ApiResponse(responseCode = "404", description = "Favorito ou usuário não encontrado")
    })
    @DeleteMapping("/{favoriteId}")
    public ResponseEntity<Void> remove(
            @PathVariable Long userId,
            @PathVariable Long favoriteId) {
        currentUserProvider.requireOwner(userId);
        favoriteService.remove(userId, favoriteId);
        return ResponseEntity.noContent().build();
    }
}
