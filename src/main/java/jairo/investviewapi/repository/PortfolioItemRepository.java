package jairo.investviewapi.repository;

import jairo.investviewapi.entity.PortfolioItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {

    List<PortfolioItem> findByPortfolioIdOrderByIdAsc(Long portfolioId);

    Optional<PortfolioItem> findByPortfolioIdAndSymbol(Long portfolioId, String symbol);

    Optional<PortfolioItem> findByIdAndPortfolioUserId(Long id, Long userId);
}
