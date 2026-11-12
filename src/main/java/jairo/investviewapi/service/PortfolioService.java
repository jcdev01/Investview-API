package jairo.investviewapi.service;

import jairo.investviewapi.dto.PortfolioItemRequest;
import jairo.investviewapi.dto.PortfolioItemResponse;
import jairo.investviewapi.dto.PortfolioItemUpdateRequest;
import jairo.investviewapi.dto.PortfolioRenameRequest;
import jairo.investviewapi.dto.PortfolioResponse;
import jairo.investviewapi.entity.Portfolio;
import jairo.investviewapi.entity.PortfolioItem;
import jairo.investviewapi.entity.User;
import jairo.investviewapi.exception.ResourceNotFoundException;
import jairo.investviewapi.repository.PortfolioItemRepository;
import jairo.investviewapi.repository.PortfolioRepository;
import jairo.investviewapi.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private static final String DEFAULT_NAME = "Minha carteira";

    private final PortfolioRepository portfolioRepository;
    private final PortfolioItemRepository portfolioItemRepository;
    private final UserRepository userRepository;

    @Transactional
    public PortfolioResponse getPortfolio(Long userId) {
        return PortfolioResponse.from(getOrCreatePortfolio(userId));
    }

    @Transactional
    public PortfolioResponse rename(Long userId, PortfolioRenameRequest request) {
        Portfolio portfolio = getOrCreatePortfolio(userId);
        portfolio.setName(request.name().trim());
        return PortfolioResponse.from(portfolioRepository.save(portfolio));
    }

    @Transactional
    public List<PortfolioItemResponse> listItems(Long userId) {
        Portfolio portfolio = getOrCreatePortfolio(userId);
        return portfolioItemRepository.findByPortfolioIdOrderByIdAsc(portfolio.getId()).stream()
                .map(PortfolioItemResponse::from)
                .toList();
    }

    @Transactional
    public PortfolioItemResponse addItem(Long userId, PortfolioItemRequest request) {
        Portfolio portfolio = getOrCreatePortfolio(userId);
        String symbol = normalizeSymbol(request.symbol());
        String name = normalizeName(request.name());

        PortfolioItem item = portfolioItemRepository
                .findByPortfolioIdAndSymbol(portfolio.getId(), symbol)
                .map(existing -> {
                    existing.setQuantity(existing.getQuantity().add(request.quantity()));
                    if (name != null) {
                        existing.setName(name);
                    }
                    return existing;
                })
                .orElseGet(() -> PortfolioItem.builder()
                        .portfolio(portfolio)
                        .symbol(symbol)
                        .name(name)
                        .quantity(request.quantity())
                        .build());

        return PortfolioItemResponse.from(portfolioItemRepository.save(item));
    }

    @Transactional
    public PortfolioItemResponse updateItem(Long userId, Long itemId, PortfolioItemUpdateRequest request) {
        PortfolioItem item = getItem(userId, itemId);
        item.setQuantity(request.quantity());
        if (request.name() != null) {
            item.setName(normalizeName(request.name()));
        }
        return PortfolioItemResponse.from(portfolioItemRepository.save(item));
    }

    @Transactional
    public void removeItem(Long userId, Long itemId) {
        portfolioItemRepository.delete(getItem(userId, itemId));
    }

    private PortfolioItem getItem(Long userId, Long itemId) {
        return portfolioItemRepository.findByIdAndPortfolioUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Item da carteira não encontrado: " + itemId));
    }

    private Portfolio getOrCreatePortfolio(Long userId) {
        return portfolioRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + userId));
                    Portfolio portfolio = Portfolio.builder()
                            .user(user)
                            .name(DEFAULT_NAME)
                            .build();
                    return portfolioRepository.save(portfolio);
                });
    }

    private static String normalizeSymbol(String symbol) {
        return symbol.trim().toUpperCase();
    }

    private static String normalizeName(String name) {
        return name != null && !name.isBlank() ? name.trim() : null;
    }
}
