package jairo.investviewapi.dto;

import jairo.investviewapi.entity.Portfolio;
import java.util.List;

public record PortfolioResponse(
        Long id,
        String name,
        List<PortfolioItemResponse> items
) {

    public static PortfolioResponse from(Portfolio portfolio) {
        return new PortfolioResponse(
                portfolio.getId(),
                portfolio.getName(),
                portfolio.getItems().stream()
                        .map(PortfolioItemResponse::from)
                        .toList());
    }
}
