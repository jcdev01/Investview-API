package jairo.investviewapi.repository;

import jairo.investviewapi.entity.AssetType;
import jairo.investviewapi.entity.Favorite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserId(Long userId);

    boolean existsByUserIdAndTypeAndSymbol(Long userId, AssetType type, String symbol);

    Optional<Favorite> findByIdAndUserId(Long id, Long userId);

    @Query("""
            select f.user.id as userId, f.type as type, f.symbol as symbol, f.name as name
            from Favorite f
            order by f.user.id, f.id
            """)
    List<FavoriteView> findAllWithUserId();
}
