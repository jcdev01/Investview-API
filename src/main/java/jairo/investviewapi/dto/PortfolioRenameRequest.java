package jairo.investviewapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PortfolioRenameRequest(

        @NotBlank
        @Size(max = 120)
        String name
) {
}
