package com.awb.ged.application.dto.folder;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFolderCommand {

    @NotBlank(message = "Folder name is required")
    private String name;

    private UUID parentFolderId;

    private UUID ownerId;
}
