package com.awb.ged.application.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of a bulk upload operation (multiple files).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUploadResultDto {

    private List<DocumentResponseDto> succeeded;
    private List<BulkUploadError> failed;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkUploadError {
        private String fileName;
        private String errorMessage;
    }
}
