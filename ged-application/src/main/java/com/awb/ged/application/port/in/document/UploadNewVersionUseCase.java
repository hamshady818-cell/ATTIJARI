package com.awb.ged.application.port.in.document;

import com.awb.ged.application.dto.document.DocumentVersionResponseDto;
import com.awb.ged.application.dto.document.UploadNewVersionCommand;

public interface UploadNewVersionUseCase {

    DocumentVersionResponseDto uploadNewVersion(UploadNewVersionCommand command);
}
