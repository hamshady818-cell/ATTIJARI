package com.awb.ged.application.service.document;

import com.awb.ged.application.dto.document.DocumentSearchQuery;
import com.awb.ged.application.dto.document.DocumentSearchResultDto;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.common.model.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SearchDocumentsServiceTest {

    @Mock
    private DocumentRepositoryPort documentRepositoryPort;

    private SearchDocumentsService searchDocumentsService;

    @BeforeEach
    void setUp() {
        searchDocumentsService = new SearchDocumentsService(documentRepositoryPort);
    }

    @Test
    @DisplayName("Should pass sanitized query to repository and return search page response")
    void search_Success() {
        // Given
        DocumentSearchQuery query = DocumentSearchQuery.builder()
                .keyword("contract")
                .page(0)
                .size(10)
                .sortBy("name")
                .sortDirection("ASC")
                .build();

        DocumentSearchResultDto resultDto = DocumentSearchResultDto.builder()
                .id(UUID.randomUUID())
                .name("Contract_2024.pdf")
                .build();

        PageResponse<DocumentSearchResultDto> expectedPage = PageResponse.<DocumentSearchResultDto>builder()
                .content(List.of(resultDto))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .sortBy("name")
                .sortDirection("ASC")
                .build();

        given(documentRepositoryPort.search(any(DocumentSearchQuery.class))).willReturn(expectedPage);

        // When
        PageResponse<DocumentSearchResultDto> result = searchDocumentsService.search(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Contract_2024.pdf");
        verify(documentRepositoryPort).search(any(DocumentSearchQuery.class));
    }
}
