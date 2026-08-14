package com.awb.ged.application.service.document;

import com.awb.ged.application.dto.document.DocumentSearchQuery;
import com.awb.ged.application.dto.document.DocumentSearchResultDto;
import com.awb.ged.application.port.in.document.SearchDocumentsUseCase;
import com.awb.ged.application.port.in.security.DocumentAccessValidator;
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
    private final DocumentAccessValidator documentAccessValidator;

    @org.springframework.beans.factory.annotation.Autowired
    public SearchDocumentsService(DocumentRepositoryPort documentRepositoryPort,
                                  DocumentAccessValidator documentAccessValidator) {
        this.documentRepositoryPort = documentRepositoryPort;
        this.documentAccessValidator = documentAccessValidator;
    }

    public SearchDocumentsService(DocumentRepositoryPort documentRepositoryPort) {
        this(documentRepositoryPort, null);
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

        PageResponse<DocumentSearchResultDto> pageResult = documentRepositoryPort.search(sanitizedQuery);
        if (documentAccessValidator == null || pageResult.getContent() == null || pageResult.getContent().isEmpty()) {
            return pageResult;
        }

        // Filter search result content by checking access permission on each document DTO
        java.util.List<DocumentSearchResultDto> filteredContent = pageResult.getContent().stream()
                .filter(dto -> {
                    try {
                        com.awb.ged.domain.document.model.Document doc = com.awb.ged.domain.document.model.Document.builder()
                                .id(dto.getId())
                                .ownerId(dto.getOwnerId())
                                .categoryId(dto.getCategoryId())
                                .departmentId(dto.getDepartmentId())
                                .build();
                        documentAccessValidator.validateAccess(doc, null, "READ");
                        return true;
                    } catch (Exception ex) {
                        return false;
                    }
                })
                .toList();

        return PageResponse.<DocumentSearchResultDto>builder()
                .content(filteredContent)
                .pageNumber(pageResult.getPageNumber())
                .pageSize(pageResult.getPageSize())
                .totalElements(filteredContent.size())
                .totalPages(filteredContent.isEmpty() ? 0 : 1)
                .first(pageResult.isFirst())
                .last(pageResult.isLast())
                .empty(filteredContent.isEmpty())
                .sortBy(pageResult.getSortBy())
                .sortDirection(pageResult.getSortDirection())
                .build();
    }
}
