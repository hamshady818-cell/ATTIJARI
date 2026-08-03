package com.awb.ged.application.port.in.document;

import com.awb.ged.application.dto.document.DocumentResponseDto;
import com.awb.ged.application.dto.document.UploadDocumentCommand;

public interface UploadDocumentUseCase {

    DocumentResponseDto uploadDocument(UploadDocumentCommand command);
}
