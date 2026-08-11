package com.awb.ged.application.service.trash;

import com.awb.ged.application.dto.trash.TrashItemResponseDto;
import com.awb.ged.application.mapper.TrashMapper;
import com.awb.ged.application.port.in.trash.GetTrashUseCase;
import com.awb.ged.application.port.out.persistence.TrashRepositoryPort;
import com.awb.ged.common.model.PageResponse;
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
    public PageResponse<TrashItemResponseDto> getTrash(UUID userId, boolean isAdminOrManager, int page, int size) {
        int validPage = Math.max(0, page);
        int validSize = size <= 0 ? 10 : Math.min(100, size);

        PageResponse<TrashItem> domainPage = trashRepositoryPort.findTrash(userId, isAdminOrManager, validPage, validSize);

        List<TrashItemResponseDto> dtoList = domainPage.getContent().stream()
                .map(trashMapper::toResponseDto)
                .toList();

        return PageResponse.<TrashItemResponseDto>builder()
                .content(dtoList)
                .pageNumber(domainPage.getPageNumber())
                .pageSize(domainPage.getPageSize())
                .totalElements(domainPage.getTotalElements())
                .totalPages(domainPage.getTotalPages())
                .sortBy(domainPage.getSortBy())
                .sortDirection(domainPage.getSortDirection())
                .first(domainPage.isFirst())
                .last(domainPage.isLast())
                .empty(domainPage.isEmpty())
                .build();
    }
}
