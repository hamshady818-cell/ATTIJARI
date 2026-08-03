package com.awb.ged.application.service.favorite;

import com.awb.ged.application.dto.favorite.FavoriteResponseDto;
import com.awb.ged.application.mapper.FavoriteMapper;
import com.awb.ged.application.port.in.favorite.GetFavoritesUseCase;
import com.awb.ged.application.port.out.persistence.FavoriteRepositoryPort;
import com.awb.ged.domain.favorite.model.Favorite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetFavoritesService implements GetFavoritesUseCase {

    private final FavoriteRepositoryPort favoriteRepositoryPort;
    private final FavoriteMapper favoriteMapper;

    @Autowired
    public GetFavoritesService(FavoriteRepositoryPort favoriteRepositoryPort,
                               FavoriteMapper favoriteMapper) {
        this.favoriteRepositoryPort = favoriteRepositoryPort;
        this.favoriteMapper = favoriteMapper;
    }

    @Override
    public List<FavoriteResponseDto> getFavorites(UUID userId) {
        List<Favorite> favorites = favoriteRepositoryPort.findByUserId(userId);
        return favorites.stream()
                .map(favoriteMapper::toResponseDto)
                .toList();
    }
}
