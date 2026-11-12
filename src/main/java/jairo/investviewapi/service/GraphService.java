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
    public List<NeighborResponse> getNeighbors(String symbol) {
        Graph graph = buildGraph();
        String id = normalizeSymbol(symbol);
        requireNode(graph, id);
        return graph.neighbors(id).stream()
                .map(node -> new NeighborResponse(
                        node.id(),
                        node.symbol(),
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
        String fromId = normalizeSymbol(from);
        String toId = normalizeSymbol(to);
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
                String id = normalizeSymbol(favorite.getSymbol());
                graph.addNode(id, id, favorite.getName());
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

    private static String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Símbolo do ativo não informado");
        }
        return symbol.trim().toUpperCase();
    }

    private static void requireNode(Graph graph, String id) {
        if (!graph.contains(id)) {
            throw new ResourceNotFoundException("Ativo não encontrado no grafo: " + id);
        }
    }
}
