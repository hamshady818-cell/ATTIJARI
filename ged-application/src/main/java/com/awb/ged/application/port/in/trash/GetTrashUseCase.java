package com.awb.ged.application.port.in.trash;

import com.awb.ged.application.dto.trash.TrashItemResponseDto;
import com.awb.ged.common.model.PageResponse;

import java.util.UUID;

public interface GetTrashUseCase {
    PageResponse<TrashItemResponseDto> getTrash(UUID userId, boolean isAdminOrManager, int page, int size);
}
