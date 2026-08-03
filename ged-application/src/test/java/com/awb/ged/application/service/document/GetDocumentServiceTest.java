package com.awb.ged.application.service.document;

import com.awb.ged.application.dto.document.DocumentResponseDto;
import com.awb.ged.application.mapper.DocumentMapper;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.document.model.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GetDocumentServiceTest {

    @Mock
    private DocumentRepositoryPort documentRepositoryPort;

    private final DocumentMapper documentMapper = Mappers.getMapper(DocumentMapper.class);

    private GetDocumentService getDocumentService;

    @BeforeEach
    void setUp() {
        getDocumentService = new GetDocumentService(documentRepositoryPort, documentMapper);
    }

    @Test
    @DisplayName("Should successfully return document DTO when ID exists")
    void getDocumentById_Success() {
        // Given
        UUID docId = UUID.randomUUID();
        Document document = Document.builder()
                .id(docId)
                .name("Invoice.pdf")
                .build();

        given(documentRepositoryPort.findById(docId)).willReturn(Optional.of(document));

        // When
        DocumentResponseDto result = getDocumentService.getDocumentById(docId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(docId);
        assertThat(result.getName()).isEqualTo("Invoice.pdf");
    }

    @Test
    @DisplayName("Should throw NotFoundException when document ID does not exist")
    void getDocumentById_NotFound_ThrowsNotFoundException() {
        // Given
        UUID docId = UUID.randomUUID();
        given(documentRepositoryPort.findById(docId)).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> getDocumentService.getDocumentById(docId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(docId.toString());
    }
}
