package com.awb.ged.application.service.document;

import com.awb.ged.application.dto.document.DocumentMetadataValueDto;
import com.awb.ged.application.dto.document.DocumentResponseDto;
import com.awb.ged.application.dto.document.UpdateDocumentCommand;
import com.awb.ged.application.mapper.DocumentMapper;
import com.awb.ged.application.port.in.security.DocumentAccessValidator;
import com.awb.ged.application.port.out.audit.AuditLogPort;
import com.awb.ged.application.port.out.persistence.CategoryRepositoryPort;
import com.awb.ged.application.port.out.persistence.DepartmentRepositoryPort;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.application.port.out.persistence.UserRepositoryPort;
import com.awb.ged.common.exception.BusinessException;
import com.awb.ged.common.exception.ConflictException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.InvalidRequestException;
import com.awb.ged.domain.category.model.Category;
import com.awb.ged.domain.department.model.Department;
import com.awb.ged.domain.document.model.Document;
import com.awb.ged.domain.document.model.DocumentLock;
import com.awb.ged.domain.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UpdateDocumentServiceTest {

    @Mock
    private DocumentRepositoryPort documentRepositoryPort;

    @Mock
    private FolderRepositoryPort folderRepositoryPort;

    @Mock
    private CategoryRepositoryPort categoryRepositoryPort;

    @Mock
    private DepartmentRepositoryPort departmentRepositoryPort;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private AuditLogPort auditLogPort;

    @Mock
    private DocumentAccessValidator documentAccessValidator;

    @Mock
    private DocumentLockGuard documentLockGuard;

    private final DocumentMapper documentMapper = Mappers.getMapper(DocumentMapper.class);

    private UpdateDocumentService updateDocumentService;

    @BeforeEach
    void setUp() {
        updateDocumentService = new UpdateDocumentService(
                documentRepositoryPort,
                folderRepositoryPort,
                categoryRepositoryPort,
                departmentRepositoryPort,
                userRepositoryPort,
                auditLogPort,
                documentMapper,
                documentAccessValidator,
                documentLockGuard
        );
    }

    @Test
    @DisplayName("Should successfully update document properties without creating a new version")
    void updateDocument_FullPropertiesSuccess() {
        // Given
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID activeVersionId = UUID.randomUUID();

        Document existingDoc = Document.builder()
                .id(docId)
                .name("Contrat Fournisseur ABC")
                .description("Ancien contrat")
                .activeVersionId(activeVersionId)
                .folderId(null)
                .build();

        UpdateDocumentCommand command = UpdateDocumentCommand.builder()
                .name("Contrat Fournisseur ABC - 2026")
                .description("Contrat de prestation informatique")
                .categoryId(categoryId)
                .departmentId(departmentId)
                .ownerId(ownerId)
                .expirationDate(LocalDate.of(2026, 12, 31))
                .tags(List.of("contrat", "fournisseur", "IT"))
                .metadata(List.of(DocumentMetadataValueDto.builder().key("Numéro de contrat").value("CTR-2026-00521").build()))
                .build();

        given(documentRepositoryPort.findById(docId)).willReturn(Optional.of(existingDoc));
        given(categoryRepositoryPort.findById(categoryId)).willReturn(Optional.of(Category.builder().id(categoryId).name("Documents juridiques").build()));
        given(departmentRepositoryPort.findById(departmentId)).willReturn(Optional.of(Department.builder().id(departmentId).name("Direction Juridique").build()));
        given(userRepositoryPort.findById(ownerId)).willReturn(Optional.of(User.builder().id(ownerId).username("sara_bennani").firstName("Sara").lastName("Bennani").build()));
        given(documentRepositoryPort.findByFolderId(null)).willReturn(List.of(existingDoc));
        given(documentRepositoryPort.save(any(Document.class))).willAnswer(inv -> inv.getArgument(0));

        // When
        DocumentResponseDto result = updateDocumentService.updateDocument(docId, command, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Contrat Fournisseur ABC - 2026");
        assertThat(result.getDescription()).isEqualTo("Contrat de prestation informatique");
        assertThat(result.getCategoryId()).isEqualTo(categoryId);
        assertThat(result.getCategoryName()).isEqualTo("Documents juridiques");
        assertThat(result.getDepartmentId()).isEqualTo(departmentId);
        assertThat(result.getDepartmentName()).isEqualTo("Direction Juridique");
        assertThat(result.getOwnerId()).isEqualTo(ownerId);
        assertThat(result.getOwnerUsername()).isEqualTo("sara_bennani");
        assertThat(result.getExpirationDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(result.getTags()).containsExactly("contrat", "fournisseur", "IT");
        assertThat(result.getActiveVersionId()).isEqualTo(activeVersionId); // Version history untouched

        verify(documentRepositoryPort).save(any(Document.class));
        verify(auditLogPort).record(eq("UPDATE_DOCUMENT_PROPERTY"), eq("DOCUMENT"), eq(docId), eq("Contrat Fournisseur ABC - 2026"), eq(userId), any());
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when name is empty")
    void updateDocument_EmptyName_ThrowsException() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Document existingDoc = Document.builder().id(docId).name("Doc.pdf").build();
        UpdateDocumentCommand command = UpdateDocumentCommand.builder().name("   ").build();

        given(documentRepositoryPort.findById(docId)).willReturn(Optional.of(existingDoc));

        assertThatThrownBy(() -> updateDocumentService.updateDocument(docId, command, userId))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Document name cannot be empty");
    }

    @Test
    @DisplayName("Should allow update when document is locked by the current user")
    void updateDocument_LockedByCurrentUser_Success() {
        // Given
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Document doc = Document.builder()
                .id(docId)
                .name("Doc.pdf")
                .lock(DocumentLock.builder()
                        .lockedBy(userId)
                        .lockedAt(Instant.now())
                        .expiration(Instant.now().plusSeconds(600))
                        .build())
                .build();
        UpdateDocumentCommand command = UpdateDocumentCommand.builder().name("UpdatedDoc.pdf").build();

        given(documentRepositoryPort.findById(docId)).willReturn(Optional.of(doc));
        willDoNothing().given(documentLockGuard).assertNotLockedByOther(docId, userId);
        given(documentRepositoryPort.findByFolderId(null)).willReturn(List.of(doc));
        given(documentRepositoryPort.save(any(Document.class))).willAnswer(inv -> inv.getArgument(0));

        // When
        DocumentResponseDto result = updateDocumentService.updateDocument(docId, command, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("UpdatedDoc.pdf");
        verify(documentLockGuard).assertNotLockedByOther(docId, userId);
        verify(documentRepositoryPort).save(any(Document.class));
    }

    @Test
    @DisplayName("Should throw DOCUMENT_LOCKED when document is locked by another user")
    void updateDocument_LockedByOtherUser_ThrowsException() {
        // Given
        UUID docId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        Document doc = Document.builder()
                .id(docId)
                .name("Doc.pdf")
                .lock(DocumentLock.builder()
                        .lockedBy(otherUserId)
                        .lockedAt(Instant.now())
                        .expiration(Instant.now().plusSeconds(600))
                        .build())
                .build();
        UpdateDocumentCommand command = UpdateDocumentCommand.builder().name("NewDoc.pdf").build();

        given(documentRepositoryPort.findById(docId)).willReturn(Optional.of(doc));
        willThrow(new BusinessException(
                ErrorCode.DOCUMENT_LOCKED,
                "Ce document est verrouillé par un autre utilisateur et ne peut pas être modifié."
        )).given(documentLockGuard).assertNotLockedByOther(docId, currentUserId);

        // When / Then
        assertThatThrownBy(() -> updateDocumentService.updateDocument(docId, command, currentUserId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("verrouillé par un autre utilisateur");

        verify(documentRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ConflictException when target name is duplicate in folder")
    void updateDocument_DuplicateName_ThrowsException() {
        UUID docId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Document existingDoc = Document.builder().id(docId).name("Doc1.pdf").folderId(null).build();
        Document duplicateDoc = Document.builder().id(otherId).name("NewName.pdf").folderId(null).build();

        UpdateDocumentCommand command = UpdateDocumentCommand.builder().name("NewName.pdf").build();

        given(documentRepositoryPort.findById(docId)).willReturn(Optional.of(existingDoc));
        given(documentRepositoryPort.findByFolderId(null)).willReturn(List.of(existingDoc, duplicateDoc));

        assertThatThrownBy(() -> updateDocumentService.updateDocument(docId, command, userId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("NewName.pdf");
    }
}
