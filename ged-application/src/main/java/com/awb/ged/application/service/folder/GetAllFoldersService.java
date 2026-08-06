package com.awb.ged.application.service.folder;

import com.awb.ged.application.dto.folder.FolderResponseDto;
import com.awb.ged.application.mapper.FolderMapper;
import com.awb.ged.application.port.in.folder.GetAllFoldersUseCase;
import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.domain.folder.model.Folder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GetAllFoldersService implements GetAllFoldersUseCase {

    private final FolderRepositoryPort folderRepositoryPort;
    private final FolderMapper folderMapper;

    @Autowired
    public GetAllFoldersService(FolderRepositoryPort folderRepositoryPort, FolderMapper folderMapper) {
        this.folderRepositoryPort = folderRepositoryPort;
        this.folderMapper = folderMapper;
    }

    @Override
    public List<FolderResponseDto> getAllFolders() {
        List<Folder> folders = folderRepositoryPort.findAll();
        return folders.stream()
                .map(folderMapper::toResponseDto)
                .toList();
    }
}
