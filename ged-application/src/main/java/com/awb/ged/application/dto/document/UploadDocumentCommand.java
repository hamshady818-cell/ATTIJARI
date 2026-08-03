package com.awb.ged.application.dto.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadDocumentCommand {

    @NotBlank(message = "Document name is required")
    private String name;

    private UUID folderId;

    private UUID categoryId;

    @NotNull(message = "Owner ID is required")
    private UUID ownerId;

    @NotBlank(message = "File MIME type is required")
    private String mimeType;

    private byte[] fileContent;
}
