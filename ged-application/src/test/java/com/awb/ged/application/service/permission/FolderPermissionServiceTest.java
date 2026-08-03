package com.awb.ged.application.service.permission;

import com.awb.ged.application.dto.permission.GrantPermissionCommand;
import com.awb.ged.application.dto.permission.PermissionResponseDto;
import com.awb.ged.application.mapper.FolderPermissionMapper;
import com.awb.ged.application.port.out.persistence.FolderPermissionRepositoryPort;
import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.common.exception.ForbiddenException;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.folder.model.Folder;
import com.awb.ged.domain.folder.model.FolderPermission;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FolderPermissionServiceTest {

    @Mock
    private FolderRepositoryPort folderRepositoryPort;

    @Mock
    private FolderPermissionRepositoryPort folderPermissionRepositoryPort;

    private final FolderPermissionMapper folderPermissionMapper = Mappers.getMapper(FolderPermissionMapper.class);

    private FolderPermissionService folderPermissionService;

    @BeforeEach
    void setUp() {
        folderPermissionService = new FolderPermissionService(folderRepositoryPort, folderPermissionRepositoryPort, folderPermissionMapper);
    }

    @Test
    @DisplayName("Should grant folder permission successfully if operator is owner")
    void grantPermission_AsOwner_Success() {
        // Given
        UUID folderId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        Folder folder = Folder.builder().id(folderId).ownerId(ownerId).build();
        GrantPermissionCommand command = GrantPermissionCommand.builder()
                .targetId(folderId)
                .userId(targetUserId)
                .canRead(true)
                .grantedBy(ownerId)
                .build();

        given(folderRepositoryPort.findById(folderId)).willReturn(Optional.of(folder));
        given(folderPermissionRepositoryPort.save(any(FolderPermission.class))).willAnswer(inv -> inv.getArgument(0));

        // When
        PermissionResponseDto result = folderPermissionService.grantPermission(command, false);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isCanRead()).isTrue();
        verify(folderPermissionRepositoryPort).save(any());
    }

    @Test
    @DisplayName("Should throw ForbiddenException when folder grant lacks authority")
    void grantPermission_Forbidden() {
        // Given
        UUID folderId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID anotherId = UUID.randomUUID();

        Folder folder = Folder.builder().id(folderId).ownerId(ownerId).build();
        GrantPermissionCommand command = GrantPermissionCommand.builder()
                .targetId(folderId)
                .grantedBy(anotherId)
                .build();

        given(folderRepositoryPort.findById(folderId)).willReturn(Optional.of(folder));

        // When / Then
        assertThatThrownBy(() -> folderPermissionService.grantPermission(command, false))
                .isInstanceOf(ForbiddenException.class);
    }
}
