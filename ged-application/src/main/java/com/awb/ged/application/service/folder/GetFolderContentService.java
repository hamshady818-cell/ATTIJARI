package com.awb.ged.application.service.folder;

import com.awb.ged.application.dto.document.DocumentResponseDto;
import com.awb.ged.application.dto.folder.FolderContentResponseDto;
import com.awb.ged.application.dto.folder.FolderResponseDto;
import com.awb.ged.application.mapper.DocumentMapper;
import com.awb.ged.application.mapper.FolderMapper;
import com.awb.ged.application.port.in.folder.GetFolderContentUseCase;
import com.awb.ged.application.port.out.event.EventPublisherPort;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.document.model.Document;
import com.awb.ged.domain.folder.model.Folder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetFolderContentService implements GetFolderContentUseCase {

    private final FolderRepositoryPort folderRepositoryPort;
    private final DocumentRepositoryPort documentRepositoryPort;
    private final FolderMapper folderMapper;
    private final DocumentMapper documentMapper;
    private final EventPublisherPort eventPublisherPort;

    @Autowired
    public GetFolderContentService(FolderRepositoryPort folderRepositoryPort,
                                  DocumentRepositoryPort documentRepositoryPort,
                                  FolderMapper folderMapper,
                                  DocumentMapper documentMapper,
                                  EventPublisherPort eventPublisherPort) {
        this.folderRepositoryPort = folderRepositoryPort;
        this.documentRepositoryPort = documentRepositoryPort;
        this.folderMapper = folderMapper;
        this.documentMapper = documentMapper;
        this.eventPublisherPort = eventPublisherPort;
    }

    public GetFolderContentService(FolderRepositoryPort folderRepositoryPort,
                                  DocumentRepositoryPort documentRepositoryPort,
                                  FolderMapper folderMapper,
                                  DocumentMapper documentMapper) {
        this(folderRepositoryPort, documentRepositoryPort, folderMapper, documentMapper, null);
    }

    @Override
    public FolderContentResponseDto getFolderContent(UUID folderId) {
        FolderResponseDto currentFolderDto = null;
        String folderName = "ROOT";

        // 1. If folderId specified, verify target folder exists
        if (folderId != null) {
            Folder currentFolder = folderRepositoryPort.findById(folderId)
                    .orElseThrow(() -> new NotFoundException(
                            ErrorCode.FOLDER_NOT_FOUND,
                            "Folder with ID " + folderId + " was not found."
                    ));
            currentFolderDto = folderMapper.toResponseDto(currentFolder);
            folderName = currentFolder.getName();
        }

        // 2. Fetch subfolders
        List<Folder> subFolders = folderRepositoryPort.findByParentId(folderId);
        List<FolderResponseDto> subFolderDtos = subFolders.stream()
                .map(folderMapper::toResponseDto)
                .toList();

        // 3. Fetch documents
        List<Document> documents = documentRepositoryPort.findByFolderId(folderId);
        List<DocumentResponseDto> documentDtos = documents.stream()
                .map(documentMapper::toResponseDto)
                .toList();

        // 4. Publish Domain Event
        if (eventPublisherPort != null) {
            eventPublisherPort.publish(new com.awb.ged.domain.folder.event.FolderViewedEvent(
                    folderId,
                    folderName,
                    null
            ));
        }

        // 5. Assemble and return response
        return FolderContentResponseDto.builder()
                .currentFolder(currentFolderDto)
                .subFolders(subFolderDtos)
                .documents(documentDtos)
                .build();
    }
}
