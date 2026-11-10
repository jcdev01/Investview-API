package jairo.investviewapi.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jairo.investviewapi.dto.GraphResponse;
import jairo.investviewapi.dto.NeighborResponse;
import jairo.investviewapi.dto.PathResponse;
import jairo.investviewapi.entity.AssetType;
import jairo.investviewapi.exception.ResourceNotFoundException;
import jairo.investviewapi.graph.AdjacencyListGraph;
import jairo.investviewapi.graph.Graph;
import jairo.investviewapi.repository.FavoriteRepository;
import jairo.investviewapi.repository.FavoriteView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GraphService {

    private final FavoriteRepository favoriteRepository;

    @Transactional(readOnly = true)
    public GraphResponse getGraph() {
        Graph graph = buildGraph();
        return new GraphResponse(graph.nodes(), graph.edges());
    }

    @Transactional(readOnly = true)
    public List<NeighborResponse> getNeighbors(AssetType type, String symbol) {
        Graph graph = buildGraph();
        String id = assetId(type, symbol.trim().toUpperCase());
        requireNode(graph, id);
        return graph.neighbors(id).stream()
                .map(node -> new NeighborResponse(
                        node.id(),
                        node.symbol(),
                        node.type(),
                        node.name(),
                        graph.weight(id, node.id())))
                .sorted(Comparator.comparingInt(NeighborResponse::weight)
                        .reversed()
                        .thenComparing(NeighborResponse::id))
                .toList();
    }

    @Transactional(readOnly = true)
    public PathResponse getShortestPath(String from, String to) {
        Graph graph = buildGraph();
        String fromId = normalizeAssetId(from);
        String toId = normalizeAssetId(to);
        requireNode(graph, fromId);
        requireNode(graph, toId);
        return graph.shortestPath(fromId, toId)
                .map(path -> new PathResponse(true, path, path.size() - 1))
                .orElseGet(() -> new PathResponse(false, List.of(), 0));
    }

    @Transactional(readOnly = true)
    public List<List<String>> getComponents() {
        return buildGraph().components().stream()
                .map(component -> component.stream().sorted().toList())
                .sorted(Comparator.comparingInt((List<String> component) -> component.size())
                        .reversed()
                        .thenComparing(component -> component.get(0)))
                .toList();
    }

    private Graph buildGraph() {
        Map<Long, List<FavoriteView>> favoritesByUser = favoriteRepository.findAllWithUserId().stream()
                .collect(Collectors.groupingBy(
                        FavoriteView::getUserId,
                        LinkedHashMap::new,
                        Collectors.toList()));

        AdjacencyListGraph graph = new AdjacencyListGraph();
        for (List<FavoriteView> favorites : favoritesByUser.values()) {
            List<String> assetIds = new ArrayList<>();
            for (FavoriteView favorite : favorites) {
                String id = assetId(favorite.getType(), favorite.getSymbol());
                graph.addNode(id, favorite.getSymbol(), favorite.getType(), favorite.getName());
                assetIds.add(id);
            }
            for (int i = 0; i < assetIds.size(); i++) {
                for (int j = i + 1; j < assetIds.size(); j++) {
                    graph.addEdge(assetIds.get(i), assetIds.get(j));
                }
            }
        }
        return graph;
    }

    private static String assetId(AssetType type, String symbol) {
        return type.name() + ":" + symbol;
    }

    private static String normalizeAssetId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Identificador do ativo não informado, use o formato TYPE:SYMBOL");
        }
        String[] parts = id.trim().split(":", 2);
        if (parts.length != 2 || parts[1].isBlank()) {
            throw new IllegalArgumentException("Identificador do ativo inválido, use TYPE:SYMBOL: " + id);
        }
        AssetType type = parseType(parts[0]);
        return assetId(type, parts[1].trim().toUpperCase());
    }

    private static AssetType parseType(String value) {
        try {
            return AssetType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Tipo de ativo inválido: " + value);
        }
    }

    private static void requireNode(Graph graph, String id) {
        if (!graph.contains(id)) {
            throw new ResourceNotFoundException("Ativo não encontrado no grafo: " + id);
        }
    }
}
