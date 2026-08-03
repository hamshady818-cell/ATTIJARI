package com.awb.ged.application.dto.folder;

import com.awb.ged.application.dto.document.DocumentResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderContentResponseDto {

    private FolderResponseDto currentFolder;
    private List<FolderResponseDto> subFolders;
    private List<DocumentResponseDto> documents;
}
