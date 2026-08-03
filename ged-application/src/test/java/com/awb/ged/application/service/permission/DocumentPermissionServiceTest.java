package com.awb.ged.application.service.permission;

import com.awb.ged.application.dto.permission.GrantPermissionCommand;
import com.awb.ged.application.dto.permission.PermissionResponseDto;
import com.awb.ged.application.mapper.DocumentPermissionMapper;
import com.awb.ged.application.port.out.persistence.DocumentPermissionRepositoryPort;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.common.exception.ForbiddenException;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.document.model.Document;
import com.awb.ged.domain.document.model.DocumentPermission;
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
class DocumentPermissionServiceTest {

    @Mock
    private DocumentRepositoryPort documentRepositoryPort;

    @Mock
    private DocumentPermissionRepositoryPort documentPermissionRepositoryPort;

    private final DocumentPermissionMapper documentPermissionMapper = Mappers.getMapper(DocumentPermissionMapper.class);

    private DocumentPermissionService documentPermissionService;

    @BeforeEach
    void setUp() {
        documentPermissionService = new DocumentPermissionService(documentRepositoryPort, documentPermissionRepositoryPort, documentPermissionMapper);
    }

    @Test
    @DisplayName("Should grant permission successfully if operator is document owner")
    void grantPermission_AsOwner_Success() {
        // Given
        UUID docId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        Document doc = Document.builder().id(docId).ownerId(ownerId).build();
        GrantPermissionCommand command = GrantPermissionCommand.builder()
                .targetId(docId)
                .userId(targetUserId)
                .canRead(true)
                .grantedBy(ownerId)
                .build();

        given(documentRepositoryPort.findById(docId)).willReturn(Optional.of(doc));
        given(documentPermissionRepositoryPort.save(any(DocumentPermission.class))).willAnswer(inv -> inv.getArgument(0));

        // When
        PermissionResponseDto result = documentPermissionService.grantPermission(command, false);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isCanRead()).isTrue();
        verify(documentPermissionRepositoryPort).save(any());
    }

    @Test
    @DisplayName("Should grant permission successfully if operator is admin/manager even if not owner")
    void grantPermission_AsAdmin_Success() {
        // Given
        UUID docId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        Document doc = Document.builder().id(docId).ownerId(ownerId).build();
        GrantPermissionCommand command = GrantPermissionCommand.builder()
                .targetId(docId)
                .userId(UUID.randomUUID())
                .canRead(true)
                .grantedBy(adminId)
                .build();

        given(documentRepositoryPort.findById(docId)).willReturn(Optional.of(doc));
        given(documentPermissionRepositoryPort.save(any(DocumentPermission.class))).willAnswer(inv -> inv.getArgument(0));

        // When
        PermissionResponseDto result = documentPermissionService.grantPermission(command, true);

        // Then
        assertThat(result).isNotNull();
        verify(documentPermissionRepositoryPort).save(any());
    }

    @Test
    @DisplayName("Should throw ForbiddenException when granting permission and not authorized")
    void grantPermission_Forbidden() {
        // Given
        UUID docId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID anotherId = UUID.randomUUID();

        Document doc = Document.builder().id(docId).ownerId(ownerId).build();
        GrantPermissionCommand command = GrantPermissionCommand.builder()
                .targetId(docId)
                .grantedBy(anotherId)
                .build();

        given(documentRepositoryPort.findById(docId)).willReturn(Optional.of(doc));

        // When / Then
        assertThatThrownBy(() -> documentPermissionService.grantPermission(command, false))
                .isInstanceOf(ForbiddenException.class);
    }
}
