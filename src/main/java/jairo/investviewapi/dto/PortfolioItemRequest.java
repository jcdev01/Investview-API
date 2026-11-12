package jairo.investviewapi.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PortfolioItemRequest(

        @NotBlank
        @Size(max = 20)
        String symbol,

        @Size(max = 120)
        String name,

        @NotNull
        @Positive
        @Digits(integer = 11, fraction = 8)
        BigDecimal quantity
) {
}
