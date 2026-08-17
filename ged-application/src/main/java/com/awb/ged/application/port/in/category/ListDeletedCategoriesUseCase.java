package com.awb.ged.application.port.in.category;

import com.awb.ged.application.dto.category.CategoryResponseDto;

import java.util.List;

public interface ListDeletedCategoriesUseCase {
    List<CategoryResponseDto> listDeletedCategories();
}
