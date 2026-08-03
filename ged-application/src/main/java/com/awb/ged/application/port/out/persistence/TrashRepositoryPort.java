package com.awb.ged.application.port.out.persistence;

import com.awb.ged.domain.trash.model.TrashItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrashRepositoryPort {

    TrashItem save(TrashItem item);

    Optional<TrashItem> findById(UUID id);

    List<TrashItem> findByDeletedByAndPurgedAtIsNull(UUID userId);

    List<TrashItem> findAllActive();

    void delete(UUID id);
}
