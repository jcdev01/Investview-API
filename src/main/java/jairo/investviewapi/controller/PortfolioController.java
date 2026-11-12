package jairo.investviewapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jairo.investviewapi.dto.PortfolioItemRequest;
import jairo.investviewapi.dto.PortfolioItemResponse;
import jairo.investviewapi.dto.PortfolioItemUpdateRequest;
import jairo.investviewapi.dto.PortfolioRenameRequest;
import jairo.investviewapi.dto.PortfolioResponse;
import jairo.investviewapi.security.CurrentUserProvider;
import jairo.investviewapi.service.PortfolioService;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Carteira", description = "Simulação da carteira de investimentos do usuário")
@RestController
@RequestMapping("/users/{userId}/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final CurrentUserProvider currentUserProvider;

    @Operation(summary = "Retorna a carteira do usuário (cria uma carteira vazia se ainda não existir)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carteira retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado à carteira de outro usuário"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping
    public ResponseEntity<PortfolioResponse> get(@PathVariable Long userId) {
        currentUserProvider.requireOwnerOrAdmin(userId);
        return ResponseEntity.ok(portfolioService.getPortfolio(userId));
    }

    @Operation(summary = "Renomeia a carteira do usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carteira renomeada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado à carteira de outro usuário"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PutMapping
    public ResponseEntity<PortfolioResponse> rename(
            @PathVariable Long userId,
            @Valid @RequestBody PortfolioRenameRequest request) {
        currentUserProvider.requireOwnerOrAdmin(userId);
        return ResponseEntity.ok(portfolioService.rename(userId, request));
    }

    @Operation(summary = "Lista as cotas da carteira do usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cotas retornadas com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado à carteira de outro usuário"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping("/items")
    public ResponseEntity<List<PortfolioItemResponse>> listItems(@PathVariable Long userId) {
        currentUserProvider.requireOwnerOrAdmin(userId);
        return ResponseEntity.ok(portfolioService.listItems(userId));
    }

    @Operation(summary = "Adiciona uma cota à carteira (soma a quantidade se a cota já existir)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cota adicionada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado à carteira de outro usuário"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PostMapping("/items")
    public ResponseEntity<PortfolioItemResponse> addItem(
            @PathVariable Long userId,
            @Valid @RequestBody PortfolioItemRequest request) {
        currentUserProvider.requireOwnerOrAdmin(userId);
        PortfolioItemResponse response = portfolioService.addItem(userId, request);
        return ResponseEntity
                .created(URI.create("/users/" + userId + "/portfolio/items/" + response.id()))
                .body(response);
    }

    @Operation(summary = "Atualiza a quantidade e/ou o nome de uma cota da carteira")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cota atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado à carteira de outro usuário"),
            @ApiResponse(responseCode = "404", description = "Cota não encontrada")
    })
    @PutMapping("/items/{itemId}")
    public ResponseEntity<PortfolioItemResponse> updateItem(
            @PathVariable Long userId,
            @PathVariable Long itemId,
            @Valid @RequestBody PortfolioItemUpdateRequest request) {
        currentUserProvider.requireOwnerOrAdmin(userId);
        return ResponseEntity.ok(portfolioService.updateItem(userId, itemId, request));
    }

    @Operation(summary = "Remove uma cota da carteira")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cota removida com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado à carteira de outro usuário"),
            @ApiResponse(responseCode = "404", description = "Cota não encontrada")
    })
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable Long userId,
            @PathVariable Long itemId) {
        currentUserProvider.requireOwnerOrAdmin(userId);
        portfolioService.removeItem(userId, itemId);
        return ResponseEntity.noContent().build();
    }
}
