package jairo.investviewapi.dto;

public record GraphEdge(
        String source,
        String target,
        int weight
) {
}
