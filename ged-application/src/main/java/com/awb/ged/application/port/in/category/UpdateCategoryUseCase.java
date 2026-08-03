package com.awb.ged.application.port.in.category;

import com.awb.ged.application.dto.category.CategoryResponseDto;
import com.awb.ged.application.dto.category.UpdateCategoryCommand;

public interface UpdateCategoryUseCase {
    CategoryResponseDto updateCategory(UpdateCategoryCommand command);
}
