package com.awb.ged.application.port.in.category;

import com.awb.ged.application.dto.category.CategoryResponseDto;
import com.awb.ged.application.dto.category.CreateCategoryCommand;

public interface CreateCategoryUseCase {
    CategoryResponseDto createCategory(CreateCategoryCommand command);
}
