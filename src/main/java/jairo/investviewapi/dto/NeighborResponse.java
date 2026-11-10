package jairo.investviewapi.dto;

import jairo.investviewapi.entity.AssetType;

public record NeighborResponse(
        String id,
        String symbol,
        AssetType type,
        String name,
        int weight
) {
}
