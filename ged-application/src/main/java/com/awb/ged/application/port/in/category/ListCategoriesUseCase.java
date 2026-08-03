package com.awb.ged.application.port.in.category;

import com.awb.ged.application.dto.category.CategoryResponseDto;

import java.util.List;
import java.util.UUID;

public interface ListCategoriesUseCase {
    List<CategoryResponseDto> listCategories(UUID parentId);
}
