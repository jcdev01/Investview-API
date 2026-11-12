package jairo.investviewapi.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PortfolioItemUpdateRequest(

        @NotNull
        @Positive
        @Digits(integer = 11, fraction = 8)
        BigDecimal quantity,

        @Size(max = 120)
        String name
) {
}
