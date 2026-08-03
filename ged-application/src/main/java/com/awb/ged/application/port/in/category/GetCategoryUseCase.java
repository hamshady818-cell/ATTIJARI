package com.awb.ged.application.port.in.category;

import com.awb.ged.application.dto.category.CategoryResponseDto;

import java.util.UUID;

public interface GetCategoryUseCase {
    CategoryResponseDto getCategoryById(UUID id);
}
