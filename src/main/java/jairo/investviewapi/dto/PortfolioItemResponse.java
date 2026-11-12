package jairo.investviewapi.dto;

import jairo.investviewapi.entity.PortfolioItem;
import java.math.BigDecimal;

public record PortfolioItemResponse(
        Long id,
        String symbol,
        String name,
        BigDecimal quantity
) {

    public static PortfolioItemResponse from(PortfolioItem item) {
        return new PortfolioItemResponse(
                item.getId(),
                item.getSymbol(),
                item.getName(),
                item.getQuantity());
    }
}
