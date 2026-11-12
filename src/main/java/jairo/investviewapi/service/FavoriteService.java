package jairo.investviewapi.service;

import jairo.investviewapi.dto.FavoriteRequest;
import jairo.investviewapi.dto.FavoriteResponse;
import jairo.investviewapi.entity.Favorite;
import jairo.investviewapi.entity.User;
import jairo.investviewapi.exception.ConflictException;
import jairo.investviewapi.exception.ResourceNotFoundException;
import jairo.investviewapi.repository.FavoriteRepository;
import jairo.investviewapi.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;

    @Transactional
    public FavoriteResponse add(Long userId, FavoriteRequest request) {
        User user = getUser(userId);
        String symbol = request.symbol().trim().toUpperCase();

        if (favoriteRepository.existsByUserIdAndSymbol(userId, symbol)) {
            throw new ConflictException("Ativo já está nos favoritos do usuário: " + symbol);
        }

        String name = request.name() != null && !request.name().isBlank()
                ? request.name().trim()
                : null;

        Favorite favorite = Favorite.builder()
                .user(user)
                .symbol(symbol)
                .name(name)
                .build();

        return FavoriteResponse.from(favoriteRepository.save(favorite));
    }

    @Transactional(readOnly = true)
    public List<FavoriteResponse> listByUser(Long userId) {
        getUser(userId);
        return favoriteRepository.findByUserId(userId).stream()
                .map(FavoriteResponse::from)
                .toList();
    }

    @Transactional
    public void remove(Long userId, Long favoriteId) {
        Favorite favorite = favoriteRepository.findByIdAndUserId(favoriteId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Favorito não encontrado: " + favoriteId));
        favoriteRepository.delete(favorite);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + userId));
    }
}
