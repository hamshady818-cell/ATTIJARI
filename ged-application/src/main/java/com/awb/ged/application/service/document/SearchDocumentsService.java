package com.awb.ged.application.service.document;

import com.awb.ged.application.dto.document.DocumentSearchQuery;
import com.awb.ged.application.dto.document.DocumentSearchResultDto;
import com.awb.ged.application.port.in.document.SearchDocumentsUseCase;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.common.model.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional(readOnly = true)
public class SearchDocumentsService implements SearchDocumentsUseCase {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("createdAt", "updatedAt", "name", "status");

    private final DocumentRepositoryPort documentRepositoryPort;

    public SearchDocumentsService(DocumentRepositoryPort documentRepositoryPort) {
        this.documentRepositoryPort = documentRepositoryPort;
    }

    @Override
    public PageResponse<DocumentSearchResultDto> search(DocumentSearchQuery query) {
        String sortField = ALLOWED_SORT_FIELDS.contains(query.getSortBy())
                ? query.getSortBy()
                : "createdAt";

        String sortDirection = "ASC".equalsIgnoreCase(query.getSortDirection()) ? "ASC" : "DESC";

        int page = Math.max(0, query.getPage());
        int size = (query.getSize() > 0 && query.getSize() <= 100) ? query.getSize() : 20;

        DocumentSearchQuery sanitizedQuery = DocumentSearchQuery.builder()
                .keyword(query.getKeyword())
                .categoryId(query.getCategoryId())
                .tagName(query.getTagName())
                .folderId(query.getFolderId())
                .ownerId(query.getOwnerId())
                .status(query.getStatus())
                .createdFrom(query.getCreatedFrom())
                .createdTo(query.getCreatedTo())
                .updatedFrom(query.getUpdatedFrom())
                .updatedTo(query.getUpdatedTo())
                .page(page)
                .size(size)
                .sortBy(sortField)
                .sortDirection(sortDirection)
                .build();

        return documentRepositoryPort.search(sanitizedQuery);
    }
}
