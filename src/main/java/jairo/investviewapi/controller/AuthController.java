package jairo.investviewapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jairo.investviewapi.dto.LoginRequest;
import jairo.investviewapi.dto.PasswordResetRequest;
import jairo.investviewapi.dto.TokenResponse;
import jairo.investviewapi.dto.UserResponse;
import jairo.investviewapi.entity.User;
import jairo.investviewapi.service.GoogleUserService;
import jairo.investviewapi.service.TokenService;
import jairo.investviewapi.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autenticação", description = "Gerenciamento de login local (e-mail/senha) e Google OAuth2")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final GoogleUserService googleUserService;
    private final TokenService tokenService;

    @Operation(summary = "Realiza o login com e-mail e senha tradicionais e retorna um JWT")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        return ResponseEntity.ok(tokenService.generate(authentication.getName()));
    }

    @Operation(summary = "Recupera/altera a senha informando o e-mail cadastrado")
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody @Valid PasswordResetRequest request) {
        userService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "Retorna o usuário autenticado a partir do token do Google")
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