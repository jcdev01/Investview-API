package jairo.investviewapi.dto;

import jairo.investviewapi.entity.AssetType;

public record GraphNode(
        String id,
        String symbol,
        AssetType type,
        String name
) {
}
