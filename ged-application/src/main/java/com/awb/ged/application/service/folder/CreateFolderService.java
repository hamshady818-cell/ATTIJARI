package com.awb.ged.application.service.folder;

import com.awb.ged.application.dto.folder.CreateFolderCommand;
import com.awb.ged.application.dto.folder.FolderResponseDto;
import com.awb.ged.application.mapper.FolderMapper;
import com.awb.ged.application.port.in.folder.CreateFolderUseCase;
import com.awb.ged.application.port.out.event.EventPublisherPort;
import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.application.port.out.persistence.UserRepositoryPort;
import com.awb.ged.common.exception.ConflictException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.folder.model.Folder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CreateFolderService implements CreateFolderUseCase {

    private final FolderRepositoryPort folderRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final FolderMapper folderMapper;
    private final EventPublisherPort eventPublisherPort;

    @Autowired
    public CreateFolderService(FolderRepositoryPort folderRepositoryPort,
                               UserRepositoryPort userRepositoryPort,
                               FolderMapper folderMapper,
                               EventPublisherPort eventPublisherPort) {
        this.folderRepositoryPort = folderRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.folderMapper = folderMapper;
        this.eventPublisherPort = eventPublisherPort;
    }

    public CreateFolderService(FolderRepositoryPort folderRepositoryPort,
                               UserRepositoryPort userRepositoryPort,
                               FolderMapper folderMapper) {
        this(folderRepositoryPort, userRepositoryPort, folderMapper, null);
    }

    @Override
    public FolderResponseDto createFolder(CreateFolderCommand command) {
        // 1. Verify parent folder existence if specified
        if (command.getParentFolderId() != null) {
            folderRepositoryPort.findById(command.getParentFolderId())
                    .orElseThrow(() -> new NotFoundException(
                            ErrorCode.FOLDER_NOT_FOUND,
                            "Parent folder with ID " + command.getParentFolderId() + " was not found."
                    ));
        }

        // 2. Verify folder name uniqueness under target parent location
        List<Folder> existingFolders = folderRepositoryPort.findByParentId(command.getParentFolderId());
        boolean nameExists = existingFolders.stream()
                .anyMatch(f -> f.getName().equalsIgnoreCase(command.getName().trim()));

        if (nameExists) {
            throw new ConflictException(
                    ErrorCode.FOLDER_DUPLICATE,
                    "A folder with name '" + command.getName().trim() + "' already exists in this location."
            );
        }

        // 3. Verify owner existence if specified
        if (command.getOwnerId() != null) {
            userRepositoryPort.findById(command.getOwnerId())
                    .orElseThrow(() -> new NotFoundException(
                            ErrorCode.USER_NOT_FOUND,
                            "User with ID " + command.getOwnerId() + " was not found."
                    ));
        }

        // 4. Construct domain Folder entity
        Instant now = Instant.now();
        Folder folderToCreate = Folder.builder()
                .id(UUID.randomUUID())
                .name(command.getName().trim())
                .parentId(command.getParentFolderId())
                .ownerId(command.getOwnerId())
                .createdAt(now)
                .updatedAt(now)
                .build();

        // 5. Save via persistence port
        Folder savedFolder = folderRepositoryPort.save(folderToCreate);

        // 6. Publish Domain Event
        if (eventPublisherPort != null) {
            eventPublisherPort.publish(new com.awb.ged.domain.folder.event.FolderCreatedEvent(
                    savedFolder.getId(),
                    savedFolder.getName(),
                    savedFolder.getParentId(),
                    savedFolder.getOwnerId()
            ));
        }

        // 7. Map and return response DTO
        return folderMapper.toResponseDto(savedFolder);
    }
}
