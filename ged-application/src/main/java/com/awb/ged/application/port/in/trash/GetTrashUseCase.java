package com.awb.ged.application.port.in.trash;

import com.awb.ged.application.dto.trash.TrashItemResponseDto;

import java.util.List;
import java.util.UUID;

public interface GetTrashUseCase {
    List<TrashItemResponseDto> getTrash(UUID userId, boolean isAdminOrManager);
}
