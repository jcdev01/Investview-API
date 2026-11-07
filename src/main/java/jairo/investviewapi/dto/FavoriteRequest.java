package jairo.investviewapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jairo.investviewapi.entity.AssetType;

public record FavoriteRequest(

        @NotBlank
        @Size(max = 20)
        String symbol,

        @NotNull
        AssetType type,

        @Size(max = 120)
        String name
) {
}
