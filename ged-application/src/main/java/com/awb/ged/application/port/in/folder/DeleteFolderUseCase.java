package com.awb.ged.application.port.in.folder;

import java.util.UUID;

public interface DeleteFolderUseCase {
    /**
     * Deletes a folder by its ID.
     *
     * @param folderId        the folder to delete
     * @param deletedByUserId the user performing the deletion
     * @param cascade         if true, also soft-deletes all contained documents and
     *                        sub-folders recursively; if false, throws ConflictException
     *                        when the folder is not empty
     */
    void deleteFolder(UUID folderId, UUID deletedByUserId, boolean cascade);
}
