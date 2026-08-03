package com.awb.ged.application.port.in.trash;

import java.util.UUID;

public interface RestoreFromTrashUseCase {
    void restoreFromTrash(UUID trashId, UUID userId);
}
