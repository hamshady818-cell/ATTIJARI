package com.awb.ged.application.port.out.persistence;

import com.awb.ged.common.model.PageResponse;
import com.awb.ged.domain.trash.model.TrashItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrashRepositoryPort {

    TrashItem save(TrashItem item);

    Optional<TrashItem> findById(UUID id);

    List<TrashItem> findByDeletedByAndPurgedAtIsNull(UUID userId);

    List<TrashItem> findAllActive();

    PageResponse<TrashItem> findTrash(UUID userId, boolean isAdminOrManager, int page, int size);

    void delete(UUID id);
}
