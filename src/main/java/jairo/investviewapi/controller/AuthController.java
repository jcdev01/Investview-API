package jairo.investviewapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jairo.investviewapi.dto.UserResponse;
import jairo.investviewapi.entity.User;
import jairo.investviewapi.service.GoogleUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticação", description = "Dados do usuário autenticado via Google")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final GoogleUserService googleUserService;

    @Operation(summary = "Retorna o usuário autenticado a partir do token do Google")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário autenticado retornado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Token sem email verificado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            throw new IllegalArgumentException("Token do Google ausente");
        }
        String email = jwt.getClaimAsString("email");
        Object emailVerifiedClaim = jwt.getClaim("email_verified");
        boolean emailVerified = emailVerifiedClaim instanceof Boolean value && value;
        if (email == null || !emailVerified) {
            throw new IllegalArgumentException("Token do Google sem email verificado");
        }
        User user = googleUserService.provision(
                email, jwt.getClaimAsString("name"), jwt.getSubject());
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
