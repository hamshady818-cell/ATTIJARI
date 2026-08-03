package com.awb.ged.infrastructure.persistence.adapter;

import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.domain.document.model.*;
import com.awb.ged.infrastructure.persistence.entity.document.DocumentCheckoutJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.document.DocumentJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.document.DocumentVersionJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.folder.FolderJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.tag.TagJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import com.awb.ged.infrastructure.persistence.repository.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Component
@Transactional
public class DocumentRepositoryAdapter implements DocumentRepositoryPort {

    private final DocumentJpaRepository documentJpaRepository;
    private final DocumentVersionJpaRepository documentVersionJpaRepository;
    private final DocumentCheckoutJpaRepository documentCheckoutJpaRepository;
    private final FolderJpaRepository folderJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final TagJpaRepository tagJpaRepository;

    public DocumentRepositoryAdapter(DocumentJpaRepository documentJpaRepository,
                                     DocumentVersionJpaRepository documentVersionJpaRepository,
                                     DocumentCheckoutJpaRepository documentCheckoutJpaRepository,
                                     FolderJpaRepository folderJpaRepository,
                                     UserJpaRepository userJpaRepository,
                                     TagJpaRepository tagJpaRepository) {
        this.documentJpaRepository = documentJpaRepository;
        this.documentVersionJpaRepository = documentVersionJpaRepository;
        this.documentCheckoutJpaRepository = documentCheckoutJpaRepository;
        this.folderJpaRepository = folderJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.tagJpaRepository = tagJpaRepository;
    }

    // --- Document Operations ---

    @Override
    public Document save(Document document) {
        DocumentJpaEntity entity = mapToEntity(document);
        DocumentJpaEntity saved = documentJpaRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Document> findById(UUID id) {
        return documentJpaRepository.findById(id)
                .filter(entity -> !entity.isDeleted())
                .map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Document> findByIdIncludingDeleted(UUID id) {
        return documentJpaRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> findByFolderId(UUID folderId) {
        List<DocumentJpaEntity> list = (folderId == null)
                ? documentJpaRepository.findByFolderIsNullAndDeletedAtIsNull()
                : documentJpaRepository.findByFolderIdAndDeletedAtIsNull(folderId);
        return list.stream().map(this::mapToDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> findAll() {
        return documentJpaRepository.findAll().stream().map(this::mapToDomain).toList();
    }

    @Override
    public void delete(UUID id) {
        documentJpaRepository.deleteById(id);
    }

    // --- Document Version Operations ---

    @Override
    public DocumentVersion saveVersion(DocumentVersion version) {
        DocumentVersionJpaEntity entity = mapVersionToEntity(version);
        DocumentVersionJpaEntity saved = documentVersionJpaRepository.save(entity);
        return mapVersionToDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DocumentVersion> findVersionById(UUID versionId) {
        return documentVersionJpaRepository.findById(versionId).map(this::mapVersionToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentVersion> findVersionsByDocumentId(UUID documentId) {
        return documentVersionJpaRepository.findByDocumentIdOrderByVersionNumberAsc(documentId)
                .stream()
                .map(this::mapVersionToDomain)
                .toList();
    }

    // --- Private Mapping Helpers ---

    private Document mapToDomain(DocumentJpaEntity entity) {
        if (entity == null) return null;

        // Construct DocumentLock if there is an active checkout (D3)
        DocumentLock lock = null;
        Optional<DocumentCheckoutJpaEntity> activeCheckout = documentCheckoutJpaRepository
                .findByDocumentIdAndCheckedInAtIsNull(entity.getId());

        if (activeCheckout.isPresent()) {
            DocumentCheckoutJpaEntity checkout = activeCheckout.get();
            lock = DocumentLock.builder()
                    .lockedBy(checkout.getCheckedOutBy() != null ? checkout.getCheckedOutBy().getId() : null)
                    .lockedAt(checkout.getCheckedOutAt())
                    .expiration(checkout.getExpectedReturnAt())
                    .build();
        }

        // Map tags (D4)
        List<DocumentTag> tags = entity.getTags().stream()
                .map(t -> DocumentTag.builder()
                        .name(t.getName())
                        .generatedByIa(false)
                        .confidence(null)
                        .build())
        .toList();

        return Document.builder()
                .id(entity.getId())
                .name(entity.getTitle()) // D1: name <-> title
                .folderId(entity.getFolder() != null ? entity.getFolder().getId() : null)
                .categoryId(null) // D2: categoryId not mapped
                .ownerId(entity.getOwner() != null ? entity.getOwner().getId() : null)
                .activeVersionId(entity.getCurrentVersion() != null ? entity.getCurrentVersion().getId() : null)
                .lock(lock) // D3: active checkout lock
                .tags(new ArrayList<>(tags))
                .createdAt(entity.getCreatedAt())
                .deletedAt(entity.getDeletedAt())
                .deletedBy(entity.getDeletedBy() != null ? entity.getDeletedBy().getId() : null)
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private DocumentJpaEntity mapToEntity(Document domain) {
        if (domain == null) return null;

        FolderJpaEntity folder = null;
        if (domain.getFolderId() != null) {
            folder = folderJpaRepository.findById(domain.getFolderId()).orElse(null);
        }

        UserJpaEntity owner = null;
        if (domain.getOwnerId() != null) {
            owner = userJpaRepository.findById(domain.getOwnerId()).orElse(null);
        }

        DocumentVersionJpaEntity currentVersion = null;
        if (domain.getActiveVersionId() != null) {
            currentVersion = documentVersionJpaRepository.findById(domain.getActiveVersionId()).orElse(null);
        }

        // Map tags
        Set<TagJpaEntity> tags = new HashSet<>();
        if (domain.getTags() != null) {
            for (DocumentTag tagDomain : domain.getTags()) {
                tagJpaRepository.findByName(tagDomain.getName())
                        .ifPresent(tags::add);
            }
        }

        UserJpaEntity deleter = null;
        if (domain.getDeletedBy() != null) {
            deleter = userJpaRepository.findById(domain.getDeletedBy()).orElse(null);
        }

        DocumentJpaEntity entity = DocumentJpaEntity.builder()
                .title(domain.getName()) // D1: name <-> title
                .folder(folder)
                .owner(owner)
                .createdBy(owner)
                .updatedBy(owner)
                .deleted(domain.isDeleted())
                .deletedAt(domain.getDeletedAt())
                .deletedBy(deleter)
                .currentVersion(currentVersion)
                .tags(tags)
                .build();

        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        return entity;
    }

    private DocumentVersion mapVersionToDomain(DocumentVersionJpaEntity entity) {
        if (entity == null) return null;

        // D5: FileReferenceId = storageBucket + "/" + storagePath
        String fileRefValue = entity.getStorageBucket() + "/" + entity.getStoragePath();

        return DocumentVersion.builder()
                .id(entity.getId())
                .documentId(entity.getDocument() != null ? entity.getDocument().getId() : null)
                .versionNumber(entity.getVersionNumber())
                .hash(entity.getChecksumSha256()) // D5: hash <-> checksumSha256
                .sizeBytes(entity.getFileSizeBytes()) // D5: sizeBytes <-> fileSizeBytes
                .fileReferenceId(new FileReferenceId(fileRefValue)) // D5: FileReferenceId
                .uploadedBy(entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null)
                .uploadedAt(entity.getCreatedAt()) // D5: uploadedAt <-> createdAt
                .build();
    }

    private DocumentVersionJpaEntity mapVersionToEntity(DocumentVersion domain) {
        if (domain == null) return null;

        DocumentJpaEntity document = null;
        if (domain.getDocumentId() != null) {
            document = documentJpaRepository.findById(domain.getDocumentId()).orElse(null);
        }

        UserJpaEntity uploader = null;
        if (domain.getUploadedBy() != null) {
            uploader = userJpaRepository.findById(domain.getUploadedBy()).orElse(null);
        }

        // D5: Parse FileReferenceId into bucket and path
        String bucket = "ged-documents";
        String path = "default-path";
        if (domain.getFileReferenceId() != null && domain.getFileReferenceId().getValue() != null) {
            String val = domain.getFileReferenceId().getValue();
            int slashIndex = val.indexOf('/');
            if (slashIndex > 0) {
                bucket = val.substring(0, slashIndex);
                path = val.substring(slashIndex + 1);
            } else {
                path = val;
            }
        }

        DocumentVersionJpaEntity entity = DocumentVersionJpaEntity.builder()
                .document(document)
                .versionNumber(domain.getVersionNumber())
                .checksumSha256(domain.getHash() != null ? domain.getHash() : "0".repeat(64))
                .fileSizeBytes(domain.getSizeBytes())
                .storageBucket(bucket)
                .storagePath(path)
                .fileReferenceId(domain.getFileReferenceId() != null ? domain.getFileReferenceId().getValue() : (bucket + "/" + path))
                .mimeType("application/octet-stream")
                .createdBy(uploader)
                .build();

        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        return entity;
    }
}
