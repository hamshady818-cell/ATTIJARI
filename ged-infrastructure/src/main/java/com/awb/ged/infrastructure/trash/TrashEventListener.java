package com.awb.ged.infrastructure.trash;

import com.awb.ged.domain.document.event.DocumentDeletedEvent;
import com.awb.ged.domain.folder.event.FolderDeletedEvent;
import com.awb.ged.infrastructure.persistence.entity.folder.FolderJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.trash.TrashJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import com.awb.ged.infrastructure.persistence.repository.FolderJpaRepository;
import com.awb.ged.infrastructure.persistence.repository.TrashJpaRepository;
import com.awb.ged.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.temporal.ChronoUnit;

@Component
public class TrashEventListener {

    private final TrashJpaRepository trashJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final FolderJpaRepository folderJpaRepository;

    @Autowired
    public TrashEventListener(TrashJpaRepository trashJpaRepository,
                              UserJpaRepository userJpaRepository,
                              FolderJpaRepository folderJpaRepository) {
        this.trashJpaRepository = trashJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.folderJpaRepository = folderJpaRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleDocumentDeleted(DocumentDeletedEvent event) {
        UserJpaEntity deletedBy = userJpaRepository.findById(event.getDeletedBy()).orElse(null);
        FolderJpaEntity originalFolder = null;
        if (event.getFolderId() != null) {
            originalFolder = folderJpaRepository.findById(event.getFolderId()).orElse(null);
        }

        TrashJpaEntity trashItem = TrashJpaEntity.builder()
                .entityType(TrashJpaEntity.EntityType.DOCUMENT)
                .entityId(event.getDocumentId())
                .originalFolder(originalFolder)
                .deletedBy(deletedBy)
                .deletedAt(event.getOccurredAt())
                .autoPurgeAt(event.getOccurredAt().plus(30, ChronoUnit.DAYS))
                .build();

        trashJpaRepository.save(trashItem);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFolderDeleted(FolderDeletedEvent event) {
        UserJpaEntity deletedBy = userJpaRepository.findById(event.getDeletedBy()).orElse(null);
        FolderJpaEntity originalFolder = null;
        if (event.getParentId() != null) {
            originalFolder = folderJpaRepository.findById(event.getParentId()).orElse(null);
        }

        TrashJpaEntity trashItem = TrashJpaEntity.builder()
                .entityType(TrashJpaEntity.EntityType.FOLDER)
                .entityId(event.getFolderId())
                .originalFolder(originalFolder)
                .deletedBy(deletedBy)
                .deletedAt(event.getOccurredAt())
                .autoPurgeAt(event.getOccurredAt().plus(30, ChronoUnit.DAYS))
                .build();

        trashJpaRepository.save(trashItem);
    }
}
