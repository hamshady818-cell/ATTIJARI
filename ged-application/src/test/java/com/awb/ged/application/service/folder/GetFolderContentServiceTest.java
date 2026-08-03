package com.awb.ged.application.service.folder;

import com.awb.ged.application.dto.folder.FolderContentResponseDto;
import com.awb.ged.application.mapper.DocumentMapper;
import com.awb.ged.application.mapper.FolderMapper;

import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.document.model.Document;
import com.awb.ged.domain.folder.model.Folder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GetFolderContentServiceTest {

    @Mock
    private FolderRepositoryPort folderRepositoryPort;

    @Mock
    private DocumentRepositoryPort documentRepositoryPort;

    private final FolderMapper folderMapper = Mappers.getMapper(FolderMapper.class);
    private final DocumentMapper documentMapper = Mappers.getMapper(DocumentMapper.class);

    private GetFolderContentService getFolderContentService;

    @BeforeEach
    void setUp() {
        getFolderContentService = new GetFolderContentService(
                folderRepositoryPort,
                documentRepositoryPort,
                folderMapper,
                documentMapper
        );
    }

    @Test
    @DisplayName("Should return root content when folderId is null")
    void getFolderContent_RootSuccess() {
        // Given
        Folder childFolder = Folder.builder().id(UUID.randomUUID()).name("SubFolder").build();
        Document rootDoc = Document.builder().id(UUID.randomUUID()).name("Doc.pdf").build();

        given(folderRepositoryPort.findByParentId(null)).willReturn(List.of(childFolder));
        given(documentRepositoryPort.findByFolderId(null)).willReturn(List.of(rootDoc));

        // When
        FolderContentResponseDto result = getFolderContentService.getFolderContent(null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCurrentFolder()).isNull();
        assertThat(result.getSubFolders()).hasSize(1);
        assertThat(result.getSubFolders().get(0).getName()).isEqualTo("SubFolder");
        assertThat(result.getDocuments()).hasSize(1);
        assertThat(result.getDocuments().get(0).getName()).isEqualTo("Doc.pdf");
    }

    @Test
    @DisplayName("Should return folder content for an existing folder ID")
    void getFolderContent_ExistingFolderSuccess() {
        // Given
        UUID folderId = UUID.randomUUID();
        Folder currentFolder = Folder.builder().id(folderId).name("Finance").build();
        Folder subFolder = Folder.builder().id(UUID.randomUUID()).name("2024").parentId(folderId).build();
        Document doc = Document.builder().id(UUID.randomUUID()).name("Invoice.pdf").folderId(folderId).build();

        given(folderRepositoryPort.findById(folderId)).willReturn(Optional.of(currentFolder));
        given(folderRepositoryPort.findByParentId(folderId)).willReturn(List.of(subFolder));
        given(documentRepositoryPort.findByFolderId(folderId)).willReturn(List.of(doc));

        // When
        FolderContentResponseDto result = getFolderContentService.getFolderContent(folderId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCurrentFolder()).isNotNull();
        assertThat(result.getCurrentFolder().getName()).isEqualTo("Finance");
        assertThat(result.getSubFolders()).hasSize(1);
        assertThat(result.getDocuments()).hasSize(1);
    }

    @Test
    @DisplayName("Should throw NotFoundException when target folder ID does not exist")
    void getFolderContent_NotFound_ThrowsNotFoundException() {
        // Given
        UUID folderId = UUID.randomUUID();
        given(folderRepositoryPort.findById(folderId)).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> getFolderContentService.getFolderContent(folderId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(folderId.toString());
    }
}
