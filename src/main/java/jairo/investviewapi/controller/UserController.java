package jairo.investviewapi.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jairo.investviewapi.dto.UserCreateRequest;
import jairo.investviewapi.dto.UserResponse;
import jairo.investviewapi.dto.UserUpdateRequest;
import jairo.investviewapi.service.UserService;

import java.net.URI;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "Usuários", description = "Endpoints administrativos de gerenciamento de usuários")

@RestController

@RequestMapping("/users")

@RequiredArgsConstructor

@PreAuthorize("hasRole('ADMIN')")

public class UserController {


    private final UserService userService;


    @Operation(summary = "Cria um novo usuário")

    @ApiResponses({

            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),

            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),

            @ApiResponse(responseCode = "403", description = "Acesso negado: restrito a administradores"),

            @ApiResponse(responseCode = "409", description = "Email já cadastrado")

    })

    @PostMapping

    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {

        UserResponse response = userService.create(request);

        return ResponseEntity

                .created(URI.create("/users/" + response.id()))

                .body(response);

    }


    @Operation(summary = "Lista todos os usuários")

    @ApiResponses({

            @ApiResponse(responseCode = "200", description = "Lista de usuários retornada com sucesso"),

            @ApiResponse(responseCode = "403", description = "Acesso negado: restrito a administradores")

    })

    @GetMapping

    public ResponseEntity<List<UserResponse>> findAll() {

        return ResponseEntity.ok(userService.findAll());

    }


    @Operation(summary = "Busca um usuário por ID")

    @ApiResponses({

            @ApiResponse(responseCode = "200", description = "Usuário retornado com sucesso"),

            @ApiResponse(responseCode = "403", description = "Acesso negado: restrito a administradores"),

            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")

    })

    @GetMapping("/{id}")

    public ResponseEntity<UserResponse> findById(@PathVariable Long id) {

        return ResponseEntity.ok(userService.findById(id));

    }


    @Operation(summary = "Atualiza um usuário por ID")

    @ApiResponses({

            @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso"),

            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),

            @ApiResponse(responseCode = "403", description = "Acesso negado: restrito a administradores"),

            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),

            @ApiResponse(responseCode = "409", description = "Email já cadastrado")

    })

    @PutMapping("/{id}")

    public ResponseEntity<UserResponse> update(

            @PathVariable Long id,

            @Valid @RequestBody UserUpdateRequest request) {

        return ResponseEntity.ok(userService.update(id, request));

    }


    @Operation(summary = "Remove um usuário por ID")

    @ApiResponses({

            @ApiResponse(responseCode = "204", description = "Usuário removido com sucesso"),

            @ApiResponse(responseCode = "403", description = "Acesso negado: restrito a administradores"),

            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")

    })

    @DeleteMapping("/{id}")

    public ResponseEntity<Void> delete(@PathVariable Long id) {

        userService.delete(id);

        return ResponseEntity.noContent().build();

    }

}