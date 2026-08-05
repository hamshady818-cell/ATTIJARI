package com.awb.ged.application.service.document;

import com.awb.ged.application.dto.document.DocumentResponseDto;
import com.awb.ged.application.dto.document.UpdateDocumentCommand;
import com.awb.ged.application.mapper.DocumentMapper;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.common.exception.ConflictException;
import com.awb.ged.domain.document.model.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UpdateDocumentServiceTest {

    @Mock
    private DocumentRepositoryPort documentRepositoryPort;

    @Mock
    private FolderRepositoryPort folderRepositoryPort;

    private final DocumentMapper documentMapper = Mappers.getMapper(DocumentMapper.class);

    private UpdateDocumentService updateDocumentService;

    @BeforeEach
    void setUp() {
        updateDocumentService = new UpdateDocumentService(documentRepositoryPort, folderRepositoryPort, documentMapper);
    }

    @Test
    @DisplayName("Should successfully rename a document when no duplicate exists")
    void updateDocument_RenameSuccess() {
        // Given
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Document existingDoc = Document.builder()
                .id(docId)
                .name("OldName.pdf")
                .folderId(null)
                .build();

        UpdateDocumentCommand command = UpdateDocumentCommand.builder()
                .newName("NewName.pdf")
                .build();

        given(documentRepositoryPort.findById(docId)).willReturn(Optional.of(existingDoc));
        given(documentRepositoryPort.findByFolderId(null)).willReturn(List.of(existingDoc));
        given(documentRepositoryPort.save(any(Document.class))).willAnswer(inv -> inv.getArgument(0));

        // When
        DocumentResponseDto result = updateDocumentService.updateDocument(docId, command, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("NewName.pdf");
        verify(documentRepositoryPort).save(any(Document.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when target name is duplicate in folder")
    void updateDocument_DuplicateName_ThrowsException() {
        // Given
        UUID docId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Document existingDoc = Document.builder().id(docId).name("Doc1.pdf").folderId(null).build();
        Document duplicateDoc = Document.builder().id(otherId).name("NewName.pdf").folderId(null).build();

        UpdateDocumentCommand command = UpdateDocumentCommand.builder().newName("NewName.pdf").build();

        given(documentRepositoryPort.findById(docId)).willReturn(Optional.of(existingDoc));
        given(documentRepositoryPort.findByFolderId(null)).willReturn(List.of(existingDoc, duplicateDoc));

        // When / Then
        assertThatThrownBy(() -> updateDocumentService.updateDocument(docId, command, userId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("NewName.pdf");
    }
}
