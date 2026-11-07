package jairo.investviewapi.repository;

import jairo.investviewapi.entity.AssetType;
import jairo.investviewapi.entity.Favorite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserId(Long userId);

    boolean existsByUserIdAndTypeAndSymbol(Long userId, AssetType type, String symbol);

    Optional<Favorite> findByIdAndUserId(Long id, Long userId);
}
