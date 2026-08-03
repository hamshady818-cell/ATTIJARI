package com.awb.ged.application.mapper;

import com.awb.ged.application.dto.favorite.FavoriteResponseDto;
import com.awb.ged.domain.favorite.model.Favorite;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FavoriteMapper {

    FavoriteResponseDto toResponseDto(Favorite favorite);
}
