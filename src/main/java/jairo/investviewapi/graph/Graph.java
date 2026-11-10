package jairo.investviewapi.graph;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import jairo.investviewapi.dto.GraphEdge;
import jairo.investviewapi.dto.GraphNode;
import jairo.investviewapi.entity.AssetType;

public interface Graph {

    void addNode(String id, String symbol, AssetType type, String name);

    void addEdge(String source, String target);

    boolean contains(String id);

    List<GraphNode> nodes();

    List<GraphNode> neighbors(String id);

    List<GraphEdge> edges();

    int weight(String source, String target);

    Optional<List<String>> shortestPath(String from, String to);

    List<Set<String>> components();
}
