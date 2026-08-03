package com.awb.ged.application.service.favorite;

import com.awb.ged.application.dto.favorite.AddFavoriteCommand;
import com.awb.ged.application.dto.favorite.FavoriteResponseDto;
import com.awb.ged.application.mapper.FavoriteMapper;
import com.awb.ged.application.port.in.favorite.AddFavoriteUseCase;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.application.port.out.persistence.FavoriteRepositoryPort;
import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.common.exception.ConflictException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.favorite.model.Favorite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class AddFavoriteService implements AddFavoriteUseCase {

    private final FavoriteRepositoryPort favoriteRepositoryPort;
    private final DocumentRepositoryPort documentRepositoryPort;
    private final FolderRepositoryPort folderRepositoryPort;
    private final FavoriteMapper favoriteMapper;

    @Autowired
    public AddFavoriteService(FavoriteRepositoryPort favoriteRepositoryPort,
                              DocumentRepositoryPort documentRepositoryPort,
                              FolderRepositoryPort folderRepositoryPort,
                              FavoriteMapper favoriteMapper) {
        this.favoriteRepositoryPort = favoriteRepositoryPort;
        this.documentRepositoryPort = documentRepositoryPort;
        this.folderRepositoryPort = folderRepositoryPort;
        this.favoriteMapper = favoriteMapper;
    }

    @Override
    public FavoriteResponseDto addFavorite(AddFavoriteCommand command) {
        // 1. Validate entity existence based on type
        String typeStr = command.getEntityType().toUpperCase();
        if ("DOCUMENT".equals(typeStr)) {
            documentRepositoryPort.findById(command.getEntityId())
                    .orElseThrow(() -> new NotFoundException(
                            ErrorCode.DOCUMENT_NOT_FOUND,
                            "Document with ID " + command.getEntityId() + " was not found."
                    ));
        } else if ("FOLDER".equals(typeStr)) {
            folderRepositoryPort.findById(command.getEntityId())
                    .orElseThrow(() -> new NotFoundException(
                            ErrorCode.FOLDER_NOT_FOUND,
                            "Folder with ID " + command.getEntityId() + " was not found."
                    ));
        } else {
            throw new IllegalArgumentException("Unsupported entity type: " + command.getEntityType());
        }

        // 2. Check if already favorited to prevent duplicate SQL constraint violation
        favoriteRepositoryPort.findByUserIdAndEntityTypeAndEntityId(command.getUserId(), typeStr, command.getEntityId())
                .ifPresent(f -> {
                    throw new ConflictException(
                            ErrorCode.INVALID_INPUT,
                            "This item is already marked as a favorite."
                    );
                });

        // 3. Create and save favorite
        Instant now = Instant.now();
        Favorite favorite = Favorite.builder()
                .id(UUID.randomUUID())
                .userId(command.getUserId())
                .entityType(typeStr)
                .entityId(command.getEntityId())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Favorite saved = favoriteRepositoryPort.save(favorite);
        return favoriteMapper.toResponseDto(saved);
    }
}
