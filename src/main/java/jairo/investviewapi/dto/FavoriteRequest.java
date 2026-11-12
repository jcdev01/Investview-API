package jairo.investviewapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FavoriteRequest(

        @NotBlank
        @Size(max = 20)
        String symbol,

        @Size(max = 120)
        String name
) {
}
