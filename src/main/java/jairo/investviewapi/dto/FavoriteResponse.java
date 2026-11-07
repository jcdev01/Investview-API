package jairo.investviewapi.dto;

import jairo.investviewapi.entity.AssetType;
import jairo.investviewapi.entity.Favorite;

public record FavoriteResponse(
        Long id,
        String symbol,
        AssetType type,
        String name
) {

    public static FavoriteResponse from(Favorite favorite) {
        return new FavoriteResponse(
                favorite.getId(),
                favorite.getSymbol(),
                favorite.getType(),
                favorite.getName());
    }
}
