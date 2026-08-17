package com.awb.ged.application.service.document;

import com.awb.ged.application.dto.document.DocumentMetadataValueDto;
import com.awb.ged.application.dto.document.DocumentResponseDto;
import com.awb.ged.application.dto.document.UpdateDocumentCommand;
import com.awb.ged.application.mapper.DocumentMapper;
import com.awb.ged.application.port.in.document.UpdateDocumentUseCase;
import com.awb.ged.application.port.in.security.DocumentAccessValidator;
import com.awb.ged.application.port.out.audit.AuditLogPort;
import com.awb.ged.application.port.out.persistence.CategoryRepositoryPort;
import com.awb.ged.application.port.out.persistence.DepartmentRepositoryPort;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.application.port.out.persistence.UserRepositoryPort;
import com.awb.ged.common.exception.BusinessException;
import com.awb.ged.common.exception.ConflictException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.InvalidRequestException;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.document.model.Document;
import com.awb.ged.domain.document.model.DocumentMetadataValue;
import com.awb.ged.domain.document.model.DocumentTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import com.awb.ged.application.port.out.persistence.MetadataDefinitionRepositoryPort;
import com.awb.ged.common.model.PageResponse;
import com.awb.ged.domain.metadata.model.MetadataDefinition;
import com.awb.ged.domain.metadata.model.MetadataType;

@Service
@Transactional
public class UpdateDocumentService implements UpdateDocumentUseCase {

    private final DocumentRepositoryPort documentRepositoryPort;
    private final FolderRepositoryPort folderRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final DepartmentRepositoryPort departmentRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final AuditLogPort auditLogPort;
    private final DocumentMapper documentMapper;
    private final DocumentAccessValidator documentAccessValidator;
    private final DocumentLockGuard documentLockGuard;
    private final MetadataDefinitionRepositoryPort metadataDefinitionRepositoryPort;

    @Autowired
    public UpdateDocumentService(DocumentRepositoryPort documentRepositoryPort,
                                 FolderRepositoryPort folderRepositoryPort,
                                 CategoryRepositoryPort categoryRepositoryPort,
                                 DepartmentRepositoryPort departmentRepositoryPort,
                                 UserRepositoryPort userRepositoryPort,
                                 AuditLogPort auditLogPort,
                                 DocumentMapper documentMapper,
                                 DocumentAccessValidator documentAccessValidator,
                                 DocumentLockGuard documentLockGuard,
                                 MetadataDefinitionRepositoryPort metadataDefinitionRepositoryPort) {
        this.documentRepositoryPort = documentRepositoryPort;
        this.folderRepositoryPort = folderRepositoryPort;
        this.categoryRepositoryPort = categoryRepositoryPort;
        this.departmentRepositoryPort = departmentRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.auditLogPort = auditLogPort;
        this.documentMapper = documentMapper;
        this.documentAccessValidator = documentAccessValidator;
        this.documentLockGuard = documentLockGuard;
        this.metadataDefinitionRepositoryPort = metadataDefinitionRepositoryPort;
    }

    public UpdateDocumentService(DocumentRepositoryPort documentRepositoryPort,
                                 FolderRepositoryPort folderRepositoryPort,
                                 CategoryRepositoryPort categoryRepositoryPort,
                                 DepartmentRepositoryPort departmentRepositoryPort,
                                 UserRepositoryPort userRepositoryPort,
                                 AuditLogPort auditLogPort,
                                 DocumentMapper documentMapper,
                                 DocumentAccessValidator documentAccessValidator,
                                 DocumentLockGuard documentLockGuard) {
        this(documentRepositoryPort, folderRepositoryPort, categoryRepositoryPort, departmentRepositoryPort, userRepositoryPort, auditLogPort, documentMapper, documentAccessValidator, documentLockGuard, null);
    }

    public UpdateDocumentService(DocumentRepositoryPort documentRepositoryPort,
                                 FolderRepositoryPort folderRepositoryPort,
                                 CategoryRepositoryPort categoryRepositoryPort,
                                 DepartmentRepositoryPort departmentRepositoryPort,
                                 UserRepositoryPort userRepositoryPort,
                                 AuditLogPort auditLogPort,
                                 DocumentMapper documentMapper) {
        this(documentRepositoryPort, folderRepositoryPort, categoryRepositoryPort, departmentRepositoryPort, userRepositoryPort, auditLogPort, documentMapper, null, null, null);
    }

    @Override
    public DocumentResponseDto updateDocument(UUID documentId, UpdateDocumentCommand command, UUID currentUserId) {
        // 1. Find document
        Document document = documentRepositoryPort.findById(documentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUND,
                        "Document with ID " + documentId + " was not found."
                ));

        if (documentAccessValidator != null) {
            documentAccessValidator.validateAccess(document, currentUserId, "WRITE");
        }

        // 2. Validate not locked by another user
        if (documentLockGuard != null) {
            documentLockGuard.assertNotLockedByOther(documentId, currentUserId);
        }

        Map<String, Object> auditChanges = new HashMap<>();

        // 3. Validate & resolve Name
        String rawName = command.getNameToUpdate();
        String targetName = document.getName();
        if (rawName != null) {
            String trimmed = rawName.trim();
            if (trimmed.isEmpty()) {
                throw new InvalidRequestException(
                        ErrorCode.INVALID_INPUT,
                        "Document name cannot be empty."
                );
            }
            if (trimmed.length() > 500) {
                throw new InvalidRequestException(
                        ErrorCode.INVALID_INPUT,
                        "Document name exceeds maximum length of 500 characters."
                );
            }
            if (!trimmed.equals(document.getName())) {
                auditChanges.put("name", Map.of("old", document.getName(), "new", trimmed));
                targetName = trimmed;
            }
        }

        // 4. Validate & resolve Folder move
        UUID targetFolderId = command.isMoveToRoot()
                ? null
                : (command.getNewFolderId() != null ? command.getNewFolderId() : document.getFolderId());

        if (targetFolderId != null && !targetFolderId.equals(document.getFolderId())) {
            folderRepositoryPort.findById(targetFolderId)
                    .orElseThrow(() -> new NotFoundException(
                            ErrorCode.FOLDER_NOT_FOUND,
                            "Target folder with ID " + targetFolderId + " was not found."
                    ));
            auditChanges.put("folderId", Map.of(
                    "old", document.getFolderId() != null ? document.getFolderId().toString() : "ROOT",
                    "new", targetFolderId.toString()
            ));
        }

        // Check name uniqueness in target folder
        if (!targetName.equalsIgnoreCase(document.getName()) || !Objects.equals(targetFolderId, document.getFolderId())) {
            final String checkName = targetName;
            List<Document> siblings = documentRepositoryPort.findByFolderId(targetFolderId);
            boolean duplicate = siblings.stream()
                    .filter(d -> !d.getId().equals(documentId))
                    .anyMatch(d -> d.getName().equalsIgnoreCase(checkName));
            if (duplicate) {
                throw new ConflictException(
                        ErrorCode.DOCUMENT_ALREADY_EXISTS,
                        "A document named '" + targetName + "' already exists in the target folder."
                );
            }
        }

        // 5. Description
        String targetDescription = document.getDescription();
        if (command.getDescription() != null && !command.getDescription().equals(document.getDescription())) {
            auditChanges.put("description", Map.of("old", String.valueOf(document.getDescription()), "new", command.getDescription()));
            targetDescription = command.getDescription();
        }

        // 6. Category
        UUID targetCategoryId = document.getCategoryId();
        if (command.getCategoryId() != null) {
            categoryRepositoryPort.findById(command.getCategoryId())
                    .orElseThrow(() -> new NotFoundException(
                            ErrorCode.CATEGORY_NOT_FOUND,
                            "Category with ID " + command.getCategoryId() + " was not found."
                    ));
            if (!command.getCategoryId().equals(document.getCategoryId())) {
                auditChanges.put("categoryId", Map.of("old", String.valueOf(document.getCategoryId()), "new", command.getCategoryId().toString()));
                targetCategoryId = command.getCategoryId();
            }
        }

        // 7. Department
        UUID targetDepartmentId = document.getDepartmentId();
        if (command.getDepartmentId() != null) {
            departmentRepositoryPort.findById(command.getDepartmentId())
                    .orElseThrow(() -> new NotFoundException(
                            ErrorCode.DEPARTMENT_NOT_FOUND,
                            "Department with ID " + command.getDepartmentId() + " was not found."
                    ));
            if (!command.getDepartmentId().equals(document.getDepartmentId())) {
                auditChanges.put("departmentId", Map.of("old", String.valueOf(document.getDepartmentId()), "new", command.getDepartmentId().toString()));
                targetDepartmentId = command.getDepartmentId();
            }
        }

        // 8. Owner / Responsable
        UUID targetOwnerId = document.getOwnerId();
        if (command.getOwnerId() != null) {
            userRepositoryPort.findById(command.getOwnerId())
                    .orElseThrow(() -> new NotFoundException(
                            ErrorCode.USER_NOT_FOUND,
                            "User with ID " + command.getOwnerId() + " was not found."
                    ));
            if (!command.getOwnerId().equals(document.getOwnerId())) {
                auditChanges.put("ownerId", Map.of("old", String.valueOf(document.getOwnerId()), "new", command.getOwnerId().toString()));
                targetOwnerId = command.getOwnerId();
            }
        }

        // 9. Expiration Date
        LocalDate targetExpirationDate = document.getExpirationDate();
        if (command.getExpirationDate() != null) {
            if (!command.getExpirationDate().equals(document.getExpirationDate())) {
                auditChanges.put("expirationDate", Map.of("old", String.valueOf(document.getExpirationDate()), "new", command.getExpirationDate().toString()));
                targetExpirationDate = command.getExpirationDate();
            }
        }

        // 10. Tags
        List<DocumentTag> targetTags = document.getTags();
        if (command.getTags() != null) {
            Set<String> uniqueTagNames = new LinkedHashSet<>();
            for (String tagStr : command.getTags()) {
                if (tagStr != null && !tagStr.trim().isEmpty()) {
                    uniqueTagNames.add(tagStr.trim());
                }
            }
            List<DocumentTag> newTagList = uniqueTagNames.stream()
                    .map(name -> DocumentTag.builder().name(name).build())
                    .toList();

            List<String> oldTagNames = document.getTags() != null ? document.getTags().stream().map(DocumentTag::getName).toList() : List.of();
            if (!oldTagNames.equals(uniqueTagNames.stream().toList())) {
                auditChanges.put("tags", Map.of("old", oldTagNames, "new", uniqueTagNames.stream().toList()));
                targetTags = new ArrayList<>(newTagList);
            }
        }

        // 11. Dynamic Metadata
        List<DocumentMetadataValue> targetMetadata = document.getMetadata();
        if (command.getMetadata() != null) {
            validateRequiredMetadata(command.getMetadata());
            List<DocumentMetadataValue> newMetadataList = new ArrayList<>();
            for (DocumentMetadataValueDto dto : command.getMetadata()) {
                if (dto.getDefinitionId() != null || (dto.getKey() != null && !dto.getKey().trim().isEmpty())) {
                    newMetadataList.add(DocumentMetadataValue.builder()
                            .definitionId(dto.getDefinitionId())
                            .key(dto.getKey())
                            .value(dto.getValue())
                            .build());
                }
            }
            auditChanges.put("metadata", Map.of("count", newMetadataList.size()));
            targetMetadata = newMetadataList;
        }

        // 12. Build updated document domain object
        Document updated = document.toBuilder()
                .name(targetName)
                .description(targetDescription)
                .folderId(targetFolderId)
                .categoryId(targetCategoryId)
                .departmentId(targetDepartmentId)
                .ownerId(targetOwnerId)
                .expirationDate(targetExpirationDate)
                .tags(targetTags)
                .metadata(targetMetadata)
                .updatedAt(Instant.now())
                .build();

        // 13. Save updated document (NO version created)
        Document saved = documentRepositoryPort.save(updated);

        // 14. Record Audit Log if changes occurred
        if (!auditChanges.isEmpty() && auditLogPort != null) {
            auditLogPort.record(
                    "UPDATE_DOCUMENT_PROPERTY",
                    "DOCUMENT",
                    saved.getId(),
                    saved.getName(),
                    currentUserId,
                    auditChanges
            );
        }

        // 15. Return updated DTO with category, department, owner usernames if available
        DocumentResponseDto response = documentMapper.toResponseDto(saved);
        if (saved != null) {
            if (saved.getCategoryId() != null) {
                categoryRepositoryPort.findById(saved.getCategoryId())
                        .ifPresent(c -> response.setCategoryName(c.getName()));
            }
            if (saved.getDepartmentId() != null) {
                departmentRepositoryPort.findById(saved.getDepartmentId())
                        .ifPresent(d -> response.setDepartmentName(d.getName()));
            }
            if (saved.getOwnerId() != null) {
                userRepositoryPort.findById(saved.getOwnerId())
                        .ifPresent(u -> {
                            response.setOwnerUsername(u.getUsername());
                            response.setOwnerName(u.getFirstName() != null ? u.getFirstName() + " " + u.getLastName() : u.getUsername());
                        });
            }
        }

        return response;
    }

    private void validateRequiredMetadata(List<DocumentMetadataValueDto> metadataDtos) {
        if (metadataDefinitionRepositoryPort == null) {
            return;
        }
        PageResponse<MetadataDefinition> activeDefsPage = metadataDefinitionRepositoryPort.findAllActive(0, 1000);
        List<MetadataDefinition> activeDefs = (activeDefsPage != null && activeDefsPage.getContent() != null)
                ? activeDefsPage.getContent()
                : Collections.emptyList();

        if (activeDefs.isEmpty()) {
            return;
        }

        Map<UUID, DocumentMetadataValueDto> dtosByDefId = new HashMap<>();
        Map<String, DocumentMetadataValueDto> dtosByKey = new HashMap<>();
        if (metadataDtos != null) {
            for (DocumentMetadataValueDto dto : metadataDtos) {
                if (dto.getDefinitionId() != null) {
                    dtosByDefId.put(dto.getDefinitionId(), dto);
                }
                if (dto.getKey() != null && !dto.getKey().trim().isEmpty()) {
                    dtosByKey.put(dto.getKey().trim().toLowerCase(), dto);
                }
            }
        }

        for (MetadataDefinition def : activeDefs) {
            if (def.isActive() && def.isRequired()) {
                DocumentMetadataValueDto dto = dtosByDefId.get(def.getId());
                if (dto == null && def.getName() != null) {
                    dto = dtosByKey.get(def.getName().trim().toLowerCase());
                }

                if (dto == null || isValueEmpty(def.getType(), dto.getValue())) {
                    throw new InvalidRequestException(
                            ErrorCode.INVALID_INPUT,
                            "La métadonnée '" + def.getLabel() + "' est obligatoire."
                    );
                }
            }
        }
    }

    private boolean isValueEmpty(MetadataType type, String value) {
        if (value == null) {
            return true;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed) || "undefined".equalsIgnoreCase(trimmed)) {
            return true;
        }
        if (type == MetadataType.BOOLEAN) {
            return !("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed));
        }
        return false;
    }
}
