package com.awb.ged.application.port.out.persistence;

import com.awb.ged.domain.document.model.DocumentPermission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentPermissionRepositoryPort {

    DocumentPermission save(DocumentPermission permission);

    Optional<DocumentPermission> findById(UUID id);

    List<DocumentPermission> findByDocumentId(UUID documentId);

    void delete(UUID id);
}
