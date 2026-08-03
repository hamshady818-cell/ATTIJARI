package com.awb.ged.application.service.folder;

import com.awb.ged.application.dto.folder.CreateFolderCommand;
import com.awb.ged.application.dto.folder.FolderResponseDto;
import com.awb.ged.application.mapper.FolderMapper;

import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.application.port.out.persistence.UserRepositoryPort;
import com.awb.ged.common.exception.ConflictException;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.folder.model.Folder;
import com.awb.ged.domain.user.model.User;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateFolderServiceTest {

    @Mock
    private FolderRepositoryPort folderRepositoryPort;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    private final FolderMapper folderMapper = Mappers.getMapper(FolderMapper.class);

    private CreateFolderService createFolderService;

    @BeforeEach
    void setUp() {
        createFolderService = new CreateFolderService(folderRepositoryPort, userRepositoryPort, folderMapper);
    }

    @Test
    @DisplayName("Should successfully create a root folder when command is valid")
    void createFolder_RootSuccess() {
        // Given
        UUID ownerId = UUID.randomUUID();
        CreateFolderCommand command = CreateFolderCommand.builder()
                .name("Finance")
                .ownerId(ownerId)
                .build();

        given(folderRepositoryPort.findByParentId(null)).willReturn(List.of());
        given(userRepositoryPort.findById(ownerId)).willReturn(Optional.of(User.builder().id(ownerId).build()));
        given(folderRepositoryPort.save(any(Folder.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        FolderResponseDto result = createFolderService.createFolder(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Finance");
        assertThat(result.getParentId()).isNull();
        assertThat(result.getOwnerId()).isEqualTo(ownerId);
        verify(folderRepositoryPort).save(any(Folder.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when folder name already exists in same location")
    void createFolder_DuplicateName_ThrowsConflictException() {
        // Given
        CreateFolderCommand command = CreateFolderCommand.builder()
                .name("Finance")
                .build();

        Folder existingFolder = Folder.builder()
                .id(UUID.randomUUID())
                .name("Finance")
                .build();

        given(folderRepositoryPort.findByParentId(null)).willReturn(List.of(existingFolder));

        // When / Then
        assertThatThrownBy(() -> createFolderService.createFolder(command))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Finance");
    }

    @Test
    @DisplayName("Should throw NotFoundException when parent folder ID does not exist")
    void createFolder_ParentNotFound_ThrowsNotFoundException() {
        // Given
        UUID parentId = UUID.randomUUID();
        CreateFolderCommand command = CreateFolderCommand.builder()
                .name("SubFinance")
                .parentFolderId(parentId)
                .build();

        given(folderRepositoryPort.findById(parentId)).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> createFolderService.createFolder(command))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(parentId.toString());
    }

    @Test
    @DisplayName("Should throw NotFoundException when owner user ID does not exist")
    void createFolder_OwnerNotFound_ThrowsNotFoundException() {
        // Given
        UUID ownerId = UUID.randomUUID();
        CreateFolderCommand command = CreateFolderCommand.builder()
                .name("Finance")
                .ownerId(ownerId)
                .build();

        given(folderRepositoryPort.findByParentId(null)).willReturn(List.of());
        given(userRepositoryPort.findById(ownerId)).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> createFolderService.createFolder(command))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(ownerId.toString());
    }
}
