package jairo.investviewapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import jairo.investviewapi.dto.GraphResponse;
import jairo.investviewapi.dto.NeighborResponse;
import jairo.investviewapi.dto.PathResponse;
import jairo.investviewapi.service.GraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Grafo", description = "Grafo global de ativos derivado da coocorrência de favoritos")
@RestController
@RequestMapping("/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphService graphService;

    @Operation(summary = "Retorna o grafo completo de ativos com nós, arestas e pesos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grafo retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    @GetMapping
    public ResponseEntity<GraphResponse> getGraph() {
        return ResponseEntity.ok(graphService.getGraph());
    }

    @Operation(summary = "Retorna os vizinhos de um ativo ordenados por peso decrescente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vizinhos retornados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Ativo não encontrado no grafo")
    })
    @GetMapping("/assets/{symbol}/neighbors")
    public ResponseEntity<List<NeighborResponse>> getNeighbors(@PathVariable String symbol) {
        return ResponseEntity.ok(graphService.getNeighbors(symbol));
    }

    @Operation(summary = "Retorna o menor caminho entre dois ativos (busca em largura)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado da busca de caminho retornado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Símbolo do ativo não informado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Ativo não encontrado no grafo")
    })
    @GetMapping("/path")
    public ResponseEntity<PathResponse> getPath(
            @RequestParam String from,
            @RequestParam String to) {
        return ResponseEntity.ok(graphService.getShortestPath(from, to));
    }

    @Operation(summary = "Retorna os componentes conexos do grafo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Componentes retornados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    @GetMapping("/components")
    public ResponseEntity<List<List<String>>> getComponents() {
        return ResponseEntity.ok(graphService.getComponents());
    }
}
