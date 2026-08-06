package com.awb.ged.application.port.in.folder;

import com.awb.ged.application.dto.folder.FolderResponseDto;
import java.util.List;

public interface GetAllFoldersUseCase {
    List<FolderResponseDto> getAllFolders();
}
