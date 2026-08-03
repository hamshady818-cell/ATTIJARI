package com.awb.ged.application.port.in.folder;

import java.util.UUID;

public interface DeleteFolderUseCase {
    void deleteFolder(UUID folderId, UUID deletedByUserId);
}
