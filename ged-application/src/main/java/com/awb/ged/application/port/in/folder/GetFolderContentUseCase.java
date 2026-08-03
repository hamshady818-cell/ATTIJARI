package com.awb.ged.application.port.in.folder;

import com.awb.ged.application.dto.folder.FolderContentResponseDto;

import java.util.UUID;

public interface GetFolderContentUseCase {

    FolderContentResponseDto getFolderContent(UUID folderId);
}
