package jairo.investviewapi.dto;

import java.util.List;

public record GraphResponse(
        List<GraphNode> nodes,
        List<GraphEdge> edges
) {
}
