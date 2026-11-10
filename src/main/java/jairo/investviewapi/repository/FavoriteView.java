package jairo.investviewapi.repository;

import jairo.investviewapi.entity.AssetType;

public interface FavoriteView {

    Long getUserId();

    AssetType getType();

    String getSymbol();

    String getName();
}
