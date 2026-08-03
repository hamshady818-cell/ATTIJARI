package com.awb.ged.application.mapper;

import com.awb.ged.application.dto.category.CategoryResponseDto;
import com.awb.ged.domain.category.model.Category;
import org.mapstruct.Mapper;

@Mapper
public interface CategoryMapper {
    CategoryResponseDto toResponseDto(Category category);
}
