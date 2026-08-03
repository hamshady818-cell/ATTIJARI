package com.awb.ged.application.service.trash;

import com.awb.ged.application.dto.trash.TrashItemResponseDto;
import com.awb.ged.application.mapper.TrashMapper;
import com.awb.ged.application.port.in.trash.GetTrashUseCase;
import com.awb.ged.application.port.out.persistence.TrashRepositoryPort;
import com.awb.ged.domain.trash.model.TrashItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetTrashService implements GetTrashUseCase {

    private final TrashRepositoryPort trashRepositoryPort;
    private final TrashMapper trashMapper;

    @Autowired
    public GetTrashService(TrashRepositoryPort trashRepositoryPort, TrashMapper trashMapper) {
        this.trashRepositoryPort = trashRepositoryPort;
        this.trashMapper = trashMapper;
    }

    @Override
    public List<TrashItemResponseDto> getTrash(UUID userId, boolean isAdminOrManager) {
        List<TrashItem> items = isAdminOrManager
                ? trashRepositoryPort.findAllActive()
                : trashRepositoryPort.findByDeletedByAndPurgedAtIsNull(userId);

        return items.stream()
                .map(trashMapper::toResponseDto)
                .toList();
    }
}
