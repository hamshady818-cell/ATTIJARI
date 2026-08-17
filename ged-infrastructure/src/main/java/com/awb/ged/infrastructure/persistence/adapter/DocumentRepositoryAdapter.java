package com.awb.ged.infrastructure.persistence.adapter;

import com.awb.ged.application.dto.document.DocumentSearchQuery;
import com.awb.ged.application.dto.document.DocumentSearchResultDto;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.domain.document.model.*;
import com.awb.ged.infrastructure.persistence.entity.document.DocumentCheckoutJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.document.DocumentJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.document.DocumentVersionJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.folder.FolderJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.tag.TagJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import com.awb.ged.infrastructure.persistence.repository.*;
import com.awb.ged.infrastructure.persistence.specification.DocumentSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import com.awb.ged.infrastructure.persistence.entity.category.CategoryJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.department.DepartmentJpaEntity;
import com.awb.ged.infrastructure.persistence.repository.*;

import com.awb.ged.infrastructure.persistence.entity.metadata.DocumentMetadataJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.metadata.MetadataDefinitionJpaEntity;

@Component
@Transactional
public class DocumentRepositoryAdapter implements DocumentRepositoryPort {

    private final DocumentJpaRepository documentJpaRepository;
    private final DocumentVersionJpaRepository documentVersionJpaRepository;
    private final DocumentCheckoutJpaRepository documentCheckoutJpaRepository;
    private final FolderJpaRepository folderJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final TagJpaRepository tagJpaRepository;
    private final CategoryJpaRepository categoryJpaRepository;
    private final DepartmentJpaRepository departmentJpaRepository;
    private final MetadataDefinitionJpaRepository metadataDefinitionJpaRepository;

    public DocumentRepositoryAdapter(DocumentJpaRepository documentJpaRepository,
                                     DocumentVersionJpaRepository documentVersionJpaRepository,
                                     DocumentCheckoutJpaRepository documentCheckoutJpaRepository,
                                     FolderJpaRepository folderJpaRepository,
                                     UserJpaRepository userJpaRepository,
                                     TagJpaRepository tagJpaRepository,
                                     CategoryJpaRepository categoryJpaRepository,
                                     DepartmentJpaRepository departmentJpaRepository,
                                     MetadataDefinitionJpaRepository metadataDefinitionJpaRepository) {
        this.documentJpaRepository = documentJpaRepository;
        this.documentVersionJpaRepository = documentVersionJpaRepository;
        this.documentCheckoutJpaRepository = documentCheckoutJpaRepository;
        this.folderJpaRepository = folderJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.tagJpaRepository = tagJpaRepository;
        this.categoryJpaRepository = categoryJpaRepository;
        this.departmentJpaRepository = departmentJpaRepository;
        this.metadataDefinitionJpaRepository = metadataDefinitionJpaRepository;
    }

    // --- Document CRUD ---

    @Override
    public Document save(Document document) {
        DocumentJpaEntity entity;

        if (document.getId() != null) {
            // Recherche de l'entité managée existante dans le Session Hibernate.
            // - Si elle EXISTE en base : on met à jour ses champs (UPDATE).
            //   Utiliser l'entité managée évite la NonUniqueObjectException.
            // - Si elle N'EXISTE PAS encore : c'est un nouveau document avec un ID
            //   pré-assigné par le service (UUID.randomUUID()). On construit via
            //   buildNewEntity() qui assigne correctement l'ID AVANT persist().
            //   NE PAS utiliser applyDomainToEntity() sur new DocumentJpaEntity() vide
            //   car BaseEntity.isNew=true + id=null → "Identifier must be manually assigned".
            java.util.Optional<DocumentJpaEntity> existing = documentJpaRepository.findById(document.getId());
            if (existing.isPresent()) {
                entity = existing.get();
                applyDomainToEntity(document, entity);
            } else {
                entity = buildNewEntity(document);
            }
        } else {
            // Cas théorique (le service devrait toujours pré-assigner un ID).
            // On génère un UUID de secours pour garantir la non-nullité.
            entity = buildNewEntity(document);
            if (entity.getId() == null) {
                entity.setId(java.util.UUID.randomUUID());
            }
        }

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

    // --- Search ---

    @Override
    @Transactional(readOnly = true)
    public com.awb.ged.common.model.PageResponse<DocumentSearchResultDto> search(DocumentSearchQuery query) {
        String sortField = "title".equals(query.getSortBy()) || "name".equals(query.getSortBy()) ? "title" : query.getSortBy();
        org.springframework.data.domain.Sort.Direction direction = "ASC".equalsIgnoreCase(query.getSortDirection())
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(query.getPage(), query.getSize(), org.springframework.data.domain.Sort.by(direction, sortField));

        Page<DocumentJpaEntity> page = documentJpaRepository.findAll(
                DocumentSpecifications.buildSearch(query), pageable);

        List<DocumentSearchResultDto> dtos = page.getContent()
                .stream()
                .map(this::mapToSearchResult)
                .toList();

        return com.awb.ged.common.model.PageResponse.<DocumentSearchResultDto>builder()
                .content(dtos)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .sortBy(query.getSortBy())
                .sortDirection(query.getSortDirection())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public List<Document> findExpiredActiveDocuments(java.time.LocalDate today) {
        return documentJpaRepository.findExpiredActiveDocuments(today)
                .stream()
                .map(this::mapToDomain)
                .toList();
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

    @Override
    @Transactional(readOnly = true)
    public int countVersionsByDocumentId(UUID documentId) {
        return documentVersionJpaRepository.countByDocumentId(documentId);
    }

    // --- Checkout / Lock Operations ---

    @Override
    public void saveCheckout(UUID documentId, UUID userId) {
        DocumentJpaEntity document = documentJpaRepository.findById(documentId)
                .orElseThrow(() -> new com.awb.ged.common.exception.NotFoundException(
                        com.awb.ged.common.exception.ErrorCode.DOCUMENT_NOT_FOUND,
                        "Document not found: " + documentId));
        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new com.awb.ged.common.exception.NotFoundException(
                        com.awb.ged.common.exception.ErrorCode.USER_NOT_FOUND,
                        "User not found: " + userId));

        DocumentCheckoutJpaEntity checkout = DocumentCheckoutJpaEntity.builder()
                .document(document)
                .checkedOutBy(user)
                .checkedOutAt(Instant.now())
                .build();
        checkout.setId(UUID.randomUUID());

        documentCheckoutJpaRepository.save(checkout);
    }

    @Override
    public void checkin(UUID documentId, UUID userId) {
        documentCheckoutJpaRepository
                .findByDocumentIdAndCheckedInAtIsNull(documentId)
                .ifPresent(checkout -> {
                    checkout.setCheckedInAt(Instant.now());
                    documentCheckoutJpaRepository.save(checkout);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CheckoutInfo> findActiveCheckout(UUID documentId) {
        return documentCheckoutJpaRepository
                .findByDocumentIdAndCheckedInAtIsNull(documentId)
                .map(c -> new CheckoutInfo(
                        c.getCheckedOutBy() != null ? c.getCheckedOutBy().getId() : null,
                        c.getCheckedOutAt(),
                        c.getExpectedReturnAt()
                ));
    }

    // --- Dashboard Stats ---

    @Override
    @Transactional(readOnly = true)
    public long countAllDocuments() {
        return documentJpaRepository.countNonDeleted();
    }

    @Override
    @Transactional(readOnly = true)
    public long sumAllVersionSizeBytes() {
        return documentJpaRepository.sumAllVersionSizeBytes();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> findRecentUploads(int limit) {
        return documentJpaRepository
                .findRecentUploads(PageRequest.of(0, limit))
                .stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> findRecentlyModified(int limit) {
        return documentJpaRepository
                .findRecentlyModified(PageRequest.of(0, limit))
                .stream()
                .map(this::mapToDomain)
                .toList();
    }

    // --- Bulk Tag ---

    @Override
    public void addTagToDocument(UUID documentId, String tagName) {
        DocumentJpaEntity document = documentJpaRepository.findById(documentId).orElse(null);
        if (document == null) return;

        TagJpaEntity tag = tagJpaRepository.findByName(tagName).orElseGet(() -> {
            TagJpaEntity newTag = new TagJpaEntity();
            newTag.setId(UUID.randomUUID());
            newTag.setName(tagName);
            return tagJpaRepository.save(newTag);
        });

        document.getTags().add(tag);
        documentJpaRepository.save(document);
    }

    // --- Private Mapping Helpers ---

    private Document mapToDomain(DocumentJpaEntity entity) {
        if (entity == null) return null;

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

        List<DocumentTag> tags = entity.getTags().stream()
                .map(t -> DocumentTag.builder()
                        .name(t.getName())
                        .generatedByIa(false)
                        .confidence(null)
                        .build())
                .toList();

        List<DocumentMetadataValue> metadata = entity.getMetadata().stream()
                .map(m -> {
                    String valStr = m.getValueText();
                    if ((valStr == null || valStr.trim().isEmpty()) && m.getValueJson() != null) {
                        if (m.getValueJson() instanceof List<?> list) {
                            valStr = list.stream().map(Object::toString).collect(Collectors.joining(","));
                        } else {
                            valStr = m.getValueJson().toString();
                        }
                    }
                    return DocumentMetadataValue.builder()
                            .definitionId(m.getDefinition() != null ? m.getDefinition().getId() : null)
                            .key(m.getDefinition() != null ? (m.getDefinition().getFieldName() != null ? m.getDefinition().getFieldName() : m.getDefinition().getDisplayLabel()) : null)
                            .value(valStr != null ? valStr : "")
                            .build();
                })
                .toList();

        // Map status from JPA enum to domain enum
        Document.DocumentStatus domainStatus = Document.DocumentStatus.DRAFT;
        if (entity.getStatus() != null) {
            try {
                domainStatus = Document.DocumentStatus.valueOf(entity.getStatus().name());
            } catch (IllegalArgumentException ignored) {}
        }

        return Document.builder()
                .id(entity.getId())
                .name(entity.getTitle())
                .description(entity.getDescription())
                .status(domainStatus)
                .mimeType(entity.getMimeType())
                .folderId(entity.getFolder() != null ? entity.getFolder().getId() : null)
                .categoryId(entity.getCategory() != null ? entity.getCategory().getId() : null)
                .departmentId(entity.getDepartment() != null ? entity.getDepartment().getId() : null)
                .ownerId(entity.getOwner() != null ? entity.getOwner().getId() : null)
                .expirationDate(entity.getExpirationDate())
                .activeVersionId(entity.getCurrentVersion() != null ? entity.getCurrentVersion().getId() : null)
                .lock(lock)
                .metadata(new ArrayList<>(metadata))
                .tags(new ArrayList<>(tags))
                .createdAt(entity.getCreatedAt())
                .deletedAt(entity.getDeletedAt())
                .deletedBy(entity.getDeletedBy() != null ? entity.getDeletedBy().getId() : null)
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private DocumentSearchResultDto mapToSearchResult(DocumentJpaEntity entity) {
        boolean locked = documentCheckoutJpaRepository
                .findByDocumentIdAndCheckedInAtIsNull(entity.getId()).isPresent();

        List<String> tagNames = entity.getTags().stream()
                .map(TagJpaEntity::getName)
                .toList();

        return DocumentSearchResultDto.builder()
                .id(entity.getId())
                .name(entity.getTitle())
                .description(entity.getDescription())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .mimeType(entity.getMimeType())
                .folderId(entity.getFolder() != null ? entity.getFolder().getId() : null)
                .folderName(entity.getFolder() != null ? entity.getFolder().getName() : null)
                .categoryId(entity.getCategory() != null ? entity.getCategory().getId() : null)
                .categoryName(entity.getCategory() != null ? entity.getCategory().getName() : null)
                .departmentId(entity.getDepartment() != null ? entity.getDepartment().getId() : null)
                .departmentName(entity.getDepartment() != null ? entity.getDepartment().getName() : null)
                .ownerId(entity.getOwner() != null ? entity.getOwner().getId() : null)
                .ownerUsername(entity.getOwner() != null ? entity.getOwner().getUsername() : null)
                .expirationDate(entity.getExpirationDate())
                .activeVersionId(entity.getCurrentVersion() != null ? entity.getCurrentVersion().getId() : null)
                .isLocked(locked)
                .tags(tagNames)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Builds a brand-new JPA entity for INSERT operations.
     * Used only when domain.getId() is null (first save of a new document).
     */
    private DocumentJpaEntity buildNewEntity(Document domain) {
        FolderJpaEntity folder = resolveFolderEntity(domain.getFolderId());
        UserJpaEntity owner = resolveUserEntity(domain.getOwnerId());
        DocumentVersionJpaEntity currentVersion = resolveVersionEntity(domain.getActiveVersionId());
        CategoryJpaEntity category = domain.getCategoryId() != null ? categoryJpaRepository.findById(domain.getCategoryId()).orElse(null) : null;
        DepartmentJpaEntity department = domain.getDepartmentId() != null ? departmentJpaRepository.findById(domain.getDepartmentId()).orElse(null) : null;
        Set<TagJpaEntity> tags = resolveTagEntities(domain);
        UserJpaEntity deleter = resolveUserEntity(domain.getDeletedBy());
        DocumentJpaEntity.DocumentStatus jpaStatus = resolveJpaStatus(domain.getStatus());

        DocumentJpaEntity entity = DocumentJpaEntity.builder()
                .title(domain.getName())
                .description(domain.getDescription())
                .status(jpaStatus)
                .mimeType(domain.getMimeType())
                .folder(folder)
                .category(category)
                .department(department)
                .owner(owner)
                .createdBy(owner)
                .updatedBy(owner)
                .expirationDate(domain.getExpirationDate())
                .deleted(domain.isDeleted())
                .deletedAt(domain.getDeletedAt())
                .deletedBy(deleter)
                .currentVersion(currentVersion)
                .tags(tags)
                .build();

        entity.setId(domain.getId() != null ? domain.getId() : UUID.randomUUID());
        applyMetadataToEntity(domain, entity);
        return entity;
    }

    /**
     * Applies domain changes to an existing managed JPA entity for UPDATE operations.
     * Only mutable fields are updated — immutable fields (createdBy, etc.) are preserved.
     * This pattern avoids Hibernate NonUniqueObjectException.
     */
    private void applyDomainToEntity(Document domain, DocumentJpaEntity entity) {
        // Mutable metadata
        if (domain.getName() != null) entity.setTitle(domain.getName());
        if (domain.getDescription() != null) entity.setDescription(domain.getDescription());
        if (domain.getMimeType() != null) entity.setMimeType(domain.getMimeType());

        // Category
        if (domain.getCategoryId() != null) {
            entity.setCategory(categoryJpaRepository.findById(domain.getCategoryId()).orElse(null));
        } else {
            entity.setCategory(null);
        }

        // Department
        if (domain.getDepartmentId() != null) {
            entity.setDepartment(departmentJpaRepository.findById(domain.getDepartmentId()).orElse(null));
        } else {
            entity.setDepartment(null);
        }

        // Owner (Responsable)
        if (domain.getOwnerId() != null) {
            UserJpaEntity owner = resolveUserEntity(domain.getOwnerId());
            if (owner != null) {
                entity.setOwner(owner);
            }
        }

        // Expiration Date
        entity.setExpirationDate(domain.getExpirationDate());

        // Status
        DocumentJpaEntity.DocumentStatus jpaStatus = resolveJpaStatus(domain.getStatus());
        entity.setStatus(jpaStatus);

        // Soft-delete fields
        entity.setDeleted(domain.isDeleted());
        entity.setDeletedAt(domain.getDeletedAt());
        entity.setDeletedBy(domain.getDeletedBy() != null ? resolveUserEntity(domain.getDeletedBy()) : null);

        // Folder (move)
        if (domain.getFolderId() != null || entity.getFolder() != null) {
            entity.setFolder(resolveFolderEntity(domain.getFolderId()));
        }

        // Active version pointer
        if (domain.getActiveVersionId() != null) {
            DocumentVersionJpaEntity currentVersion = resolveVersionEntity(domain.getActiveVersionId());
            entity.setCurrentVersion(currentVersion);
        }

        // Tags
        Set<TagJpaEntity> tags = resolveTagEntities(domain);
        entity.setTags(tags);

        // Dynamic Metadata
        applyMetadataToEntity(domain, entity);
    }

    private void applyMetadataToEntity(Document domain, DocumentJpaEntity entity) {
        if (domain.getMetadata() == null) {
            return;
        }

        UserJpaEntity editor = entity.getUpdatedBy() != null
                ? entity.getUpdatedBy()
                : (entity.getOwner() != null ? entity.getOwner() : entity.getCreatedBy());

        Map<UUID, DocumentMetadataJpaEntity> existingMap = new HashMap<>();
        if (entity.getMetadata() != null) {
            for (DocumentMetadataJpaEntity existingMeta : entity.getMetadata()) {
                if (existingMeta.getDefinition() != null && existingMeta.getDefinition().getId() != null) {
                    existingMap.put(existingMeta.getDefinition().getId(), existingMeta);
                }
            }
        } else {
            entity.setMetadata(new ArrayList<>());
        }

        List<DocumentMetadataJpaEntity> updatedList = new ArrayList<>();

        for (DocumentMetadataValue val : domain.getMetadata()) {
            if (val == null) continue;

            MetadataDefinitionJpaEntity def = null;
            if (val.getDefinitionId() != null) {
                def = metadataDefinitionJpaRepository.findById(val.getDefinitionId()).orElse(null);
            }
            if (def == null && val.getKey() != null && !val.getKey().trim().isEmpty()) {
                def = metadataDefinitionJpaRepository.findByFieldNameAndDeletedAtIsNull(val.getKey().trim()).orElse(null);
            }

            if (def == null) continue;

            String valStr = val.getValue() != null ? val.getValue() : "";

            Object valJson = null;
            if (def.getFieldType() == MetadataDefinitionJpaEntity.FieldType.MULTI_SELECT ||
                valStr.startsWith("[") || valStr.contains(",")) {
                if (valStr.startsWith("[") && valStr.endsWith("]")) {
                    valJson = valStr;
                } else if (!valStr.isEmpty()) {
                    List<String> items = Arrays.stream(valStr.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList();
                    valJson = items;
                }
            }

            DocumentMetadataJpaEntity metaEntity = existingMap.get(def.getId());
            if (metaEntity != null) {
                metaEntity.setValueText(valStr);
                metaEntity.setValueJson(valJson);
                if (editor != null) metaEntity.setUpdatedBy(editor);
            } else {
                metaEntity = DocumentMetadataJpaEntity.builder()
                        .document(entity)
                        .definition(def)
                        .valueText(valStr)
                        .valueJson(valJson)
                        .updatedBy(editor)
                        .build();
                metaEntity.setId(UUID.randomUUID());
            }
            updatedList.add(metaEntity);
        }

        entity.getMetadata().removeIf(existing -> !updatedList.contains(existing));
        for (DocumentMetadataJpaEntity newMeta : updatedList) {
            if (!entity.getMetadata().contains(newMeta)) {
                entity.getMetadata().add(newMeta);
            }
        }
    }

    // --- Helper resolvers (avoid code duplication) ---

    private FolderJpaEntity resolveFolderEntity(UUID folderId) {
        if (folderId == null) return null;
        return folderJpaRepository.findById(folderId).orElse(null);
    }

    private UserJpaEntity resolveUserEntity(UUID userId) {
        if (userId == null) return null;
        return userJpaRepository.findById(userId).orElse(null);
    }

    private DocumentVersionJpaEntity resolveVersionEntity(UUID versionId) {
        if (versionId == null) return null;
        return documentVersionJpaRepository.findById(versionId).orElse(null);
    }

    private Set<TagJpaEntity> resolveTagEntities(Document domain) {
        Set<TagJpaEntity> tags = new HashSet<>();
        if (domain.getTags() != null) {
            for (DocumentTag tagDomain : domain.getTags()) {
                if (tagDomain.getName() != null) {
                    String tagName = tagDomain.getName().trim();
                    if (!tagName.isEmpty()) {
                        TagJpaEntity tagEntity = tagJpaRepository.findByName(tagName)
                                .orElseGet(() -> tagJpaRepository.save(TagJpaEntity.builder()
                                        .name(tagName)
                                        .color("#0070B8")
                                        .build()));
                        tags.add(tagEntity);
                    }
                }
            }
        }
        return tags;
    }

    private DocumentJpaEntity.DocumentStatus resolveJpaStatus(Document.DocumentStatus status) {
        if (status == null) return DocumentJpaEntity.DocumentStatus.DRAFT;
        try {
            return DocumentJpaEntity.DocumentStatus.valueOf(status.name());
        } catch (IllegalArgumentException ignored) {
            return DocumentJpaEntity.DocumentStatus.DRAFT;
        }
    }

    private DocumentVersion mapVersionToDomain(DocumentVersionJpaEntity entity) {
        if (entity == null) return null;

        String fileRefValue = entity.getStorageBucket() + "/" + entity.getStoragePath();

        return DocumentVersion.builder()
                .id(entity.getId())
                .documentId(entity.getDocument() != null ? entity.getDocument().getId() : null)
                .versionNumber(entity.getVersionNumber())
                .hash(entity.getChecksumSha256())
                .sizeBytes(entity.getFileSizeBytes())
                .mimeType(entity.getMimeType())
                .fileReferenceId(new FileReferenceId(fileRefValue))
                .uploadedBy(entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null)
                .uploadedAt(entity.getCreatedAt())
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

        // BUG 2 FIX: Utiliser le mimeType réel du domaine, jamais un hardcode "application/octet-stream"
        String mimeType = (domain.getMimeType() != null && !domain.getMimeType().isBlank())
                ? domain.getMimeType()
                : "application/octet-stream";

        DocumentVersionJpaEntity entity = DocumentVersionJpaEntity.builder()
                .document(document)
                .versionNumber(domain.getVersionNumber())
                .checksumSha256(domain.getHash() != null ? domain.getHash() : "0".repeat(64))
                .fileSizeBytes(domain.getSizeBytes())
                .storageBucket(bucket)
                .storagePath(path)
                .fileReferenceId(domain.getFileReferenceId() != null
                        ? domain.getFileReferenceId().getValue()
                        : (bucket + "/" + path))
                .mimeType(mimeType)
                .createdBy(uploader)
                .build();

        entity.setId(domain.getId() != null ? domain.getId() : UUID.randomUUID());
        return entity;
    }
}


