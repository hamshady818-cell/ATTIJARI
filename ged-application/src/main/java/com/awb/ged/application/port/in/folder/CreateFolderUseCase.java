package com.awb.ged.application.port.in.folder;

import com.awb.ged.application.dto.folder.CreateFolderCommand;
import com.awb.ged.application.dto.folder.FolderResponseDto;

public interface CreateFolderUseCase {

    FolderResponseDto createFolder(CreateFolderCommand command);
}
