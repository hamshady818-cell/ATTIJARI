package com.awb.ged.application.port.in.document;

import com.awb.ged.application.dto.document.DocumentSearchQuery;
import com.awb.ged.application.dto.document.DocumentSearchResultDto;
import com.awb.ged.common.model.PageResponse;

public interface SearchDocumentsUseCase {

    PageResponse<DocumentSearchResultDto> search(DocumentSearchQuery query);
}
