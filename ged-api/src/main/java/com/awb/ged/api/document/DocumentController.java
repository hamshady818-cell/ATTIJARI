package com.awb.ged.api.document;

import com.awb.ged.api.permission.dto.GrantPermissionRequest;
import com.awb.ged.application.dto.document.*;
import com.awb.ged.application.dto.permission.GrantPermissionCommand;
import com.awb.ged.application.dto.permission.PermissionResponseDto;
import com.awb.ged.application.port.in.document.*;
import com.awb.ged.application.port.in.security.CurrentUserProvider;
import com.awb.ged.application.port.out.persistence.UserRepositoryPort;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.InvalidRequestException;
import com.awb.ged.common.security.CurrentUser;
import com.awb.ged.domain.user.model.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // docx
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",      // xlsx
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/tiff",
            "text/plain",
            "text/html"
    );

    private final UploadDocumentUseCase uploadDocumentUseCase;
    private final GetDocumentUseCase getDocumentUseCase;
    private final DeleteDocumentUseCase deleteDocumentUseCase;
    private final GrantDocumentPermissionUseCase grantDocumentPermissionUseCase;
    private final RevokeDocumentPermissionUseCase revokeDocumentPermissionUseCase;
    private final ListDocumentPermissionsUseCase listDocumentPermissionsUseCase;
    private final SearchDocumentsUseCase searchDocumentsUseCase;
    private final DownloadDocumentUseCase downloadDocumentUseCase;
    private final PreviewDocumentUseCase previewDocumentUseCase;
    private final UploadNewVersionUseCase uploadNewVersionUseCase;
    private final ListDocumentVersionsUseCase listDocumentVersionsUseCase;
    private final CheckoutDocumentUseCase checkoutDocumentUseCase;
    private final CheckinDocumentUseCase checkinDocumentUseCase;
    private final GetDocumentLockUseCase getDocumentLockUseCase;
    private final UpdateDocumentUseCase updateDocumentUseCase;
    private final UpdateDocumentStatusUseCase updateDocumentStatusUseCase;
    private final BulkDocumentActionUseCase bulkDocumentActionUseCase;

    private final CurrentUserProvider currentUserProvider;
    private final UserRepositoryPort userRepositoryPort;
    private final long maxFileSize;

    public DocumentController(UploadDocumentUseCase uploadDocumentUseCase,
                              GetDocumentUseCase getDocumentUseCase,
                              DeleteDocumentUseCase deleteDocumentUseCase,
                              GrantDocumentPermissionUseCase grantDocumentPermissionUseCase,
                              RevokeDocumentPermissionUseCase revokeDocumentPermissionUseCase,
                              ListDocumentPermissionsUseCase listDocumentPermissionsUseCase,
                              SearchDocumentsUseCase searchDocumentsUseCase,
                              DownloadDocumentUseCase downloadDocumentUseCase,
                              PreviewDocumentUseCase previewDocumentUseCase,
                              UploadNewVersionUseCase uploadNewVersionUseCase,
                              ListDocumentVersionsUseCase listDocumentVersionsUseCase,
                              CheckoutDocumentUseCase checkoutDocumentUseCase,
                              CheckinDocumentUseCase checkinDocumentUseCase,
                              GetDocumentLockUseCase getDocumentLockUseCase,
                              UpdateDocumentUseCase updateDocumentUseCase,
                              UpdateDocumentStatusUseCase updateDocumentStatusUseCase,
                              BulkDocumentActionUseCase bulkDocumentActionUseCase,
                              CurrentUserProvider currentUserProvider,
                              UserRepositoryPort userRepositoryPort,
                              @Value("${ged.upload.max-file-size:52428800}") long maxFileSize) {
        this.uploadDocumentUseCase = uploadDocumentUseCase;
        this.getDocumentUseCase = getDocumentUseCase;
        this.deleteDocumentUseCase = deleteDocumentUseCase;
        this.grantDocumentPermissionUseCase = grantDocumentPermissionUseCase;
        this.revokeDocumentPermissionUseCase = revokeDocumentPermissionUseCase;
        this.listDocumentPermissionsUseCase = listDocumentPermissionsUseCase;
        this.searchDocumentsUseCase = searchDocumentsUseCase;
        this.downloadDocumentUseCase = downloadDocumentUseCase;
        this.previewDocumentUseCase = previewDocumentUseCase;
        this.uploadNewVersionUseCase = uploadNewVersionUseCase;
        this.listDocumentVersionsUseCase = listDocumentVersionsUseCase;
        this.checkoutDocumentUseCase = checkoutDocumentUseCase;
        this.checkinDocumentUseCase = checkinDocumentUseCase;
        this.getDocumentLockUseCase = getDocumentLockUseCase;
        this.updateDocumentUseCase = updateDocumentUseCase;
        this.updateDocumentStatusUseCase = updateDocumentStatusUseCase;
        this.bulkDocumentActionUseCase = bulkDocumentActionUseCase;
        this.currentUserProvider = currentUserProvider;
        this.userRepositoryPort = userRepositoryPort;
        this.maxFileSize = maxFileSize;
    }

    // =========================================================================
    // 1. SEARCH
    // =========================================================================

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('DOCUMENT_READ') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER') or hasRole('VIEWER')")
    public ResponseEntity<com.awb.ged.common.model.PageResponse<DocumentSearchResultDto>> searchDocuments(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) UUID categoryId,
            @RequestParam(value = "tagName", required = false) String tagName,
            @RequestParam(value = "folderId", required = false) UUID folderId,
            @RequestParam(value = "ownerId", required = false) UUID ownerId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "createdFrom", required = false) Instant createdFrom,
            @RequestParam(value = "createdTo", required = false) Instant createdTo,
            @RequestParam(value = "updatedFrom", required = false) Instant updatedFrom,
            @RequestParam(value = "updatedTo", required = false) Instant updatedTo,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "DESC") String sortDirection) {

        DocumentSearchQuery query = DocumentSearchQuery.builder()
                .keyword(keyword)
                .categoryId(categoryId)
                .tagName(tagName)
                .folderId(folderId)
                .ownerId(ownerId)
                .status(status)
                .createdFrom(createdFrom)
                .createdTo(createdTo)
                .updatedFrom(updatedFrom)
                .updatedTo(updatedTo)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        com.awb.ged.common.model.PageResponse<DocumentSearchResultDto> results = searchDocumentsUseCase.search(query);
        return ResponseEntity.ok(results);
    }


    // =========================================================================
    // 2. UPLOAD & BULK UPLOAD
    // =========================================================================

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('DOCUMENT_CREATE') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<DocumentResponseDto> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam(value = "folderId", required = false) UUID folderId,
            @RequestParam(value = "categoryId", required = false) UUID categoryId) {

        validateFile(file);

        if (name == null || name.trim().isBlank()) {
            throw new InvalidRequestException(ErrorCode.INVALID_INPUT, "Document name is required.");
        }

        UUID ownerId = resolveCurrentUserId();
        byte[] fileBytes = extractBytes(file);

        UploadDocumentCommand command = UploadDocumentCommand.builder()
                .name(name.trim())
                .folderId(folderId)
                .categoryId(categoryId)
                .ownerId(ownerId)
                .mimeType(file.getContentType())
                .fileContent(fileBytes)
                .build();

        DocumentResponseDto created = uploadDocumentUseCase.uploadDocument(command);
        URI location = URI.create("/api/v1/documents/" + created.getId());

        return ResponseEntity.created(location).body(created);
    }

    @PostMapping(value = "/upload/bulk", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('DOCUMENT_CREATE') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<BulkUploadResultDto> bulkUploadDocuments(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "folderId", required = false) UUID folderId,
            @RequestParam(value = "categoryId", required = false) UUID categoryId) {

        if (files == null || files.length == 0) {
            throw new InvalidRequestException(ErrorCode.INVALID_INPUT, "At least one file must be provided for bulk upload.");
        }

        UUID ownerId = resolveCurrentUserId();
        List<DocumentResponseDto> succeeded = new ArrayList<>();
        List<BulkUploadResultDto.BulkUploadError> failed = new ArrayList<>();

        for (MultipartFile file : files) {
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed_file";
            try {
                validateFile(file);
                byte[] fileBytes = extractBytes(file);

                UploadDocumentCommand command = UploadDocumentCommand.builder()
                        .name(filename)
                        .folderId(folderId)
                        .categoryId(categoryId)
                        .ownerId(ownerId)
                        .mimeType(file.getContentType())
                        .fileContent(fileBytes)
                        .build();

                DocumentResponseDto created = uploadDocumentUseCase.uploadDocument(command);
                succeeded.add(created);
            } catch (Exception e) {
                failed.add(new BulkUploadResultDto.BulkUploadError(filename, e.getMessage()));
            }
        }

        BulkUploadResultDto result = BulkUploadResultDto.builder()
                .succeeded(succeeded)
                .failed(failed)
                .build();

        return ResponseEntity.status(failed.isEmpty() ? HttpStatus.CREATED : HttpStatus.MULTI_STATUS).body(result);
    }

    // =========================================================================
    // 3. READ, DOWNLOAD & PREVIEW
    // =========================================================================

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENT_READ') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER') or hasRole('VIEWER')")
    public ResponseEntity<DocumentResponseDto> getDocumentById(@PathVariable("id") UUID id) {
        DocumentResponseDto document = getDocumentUseCase.getDocumentById(id);
        return ResponseEntity.ok(document);
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('DOCUMENT_READ') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER') or hasRole('VIEWER')")
    public ResponseEntity<StreamingResponseBody> downloadDocument(@PathVariable("id") UUID id) {
        DownloadDocumentUseCase.DownloadResult result = downloadDocumentUseCase.download(id, null);
        return buildStreamingDownloadResponse(result);
    }

    @GetMapping("/{id}/preview")
    @PreAuthorize("hasAuthority('DOCUMENT_READ') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER') or hasRole('VIEWER')")
    public ResponseEntity<StreamingResponseBody> previewDocument(@PathVariable("id") UUID id) {
        PreviewDocumentUseCase.PreviewResult result = previewDocumentUseCase.preview(id);

        StreamingResponseBody body = outputStream -> {
            try (InputStream in = result.inputStream()) {
                in.transferTo(outputStream);
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(result.fileName()).build().toString())
                .contentLength(result.sizeBytes())
                .body(body);
    }

    // =========================================================================
    // 4. VERSION MANAGEMENT & HISTORY DOWNLOAD
    // =========================================================================

    @PostMapping(value = "/{id}/versions", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('DOCUMENT_WRITE') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<DocumentVersionResponseDto> uploadNewVersion(
            @PathVariable("id") UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "changeSummary", required = false) String changeSummary) {

        validateFile(file);
        UUID currentUserId = resolveCurrentUserId();
        byte[] fileBytes = extractBytes(file);

        UploadNewVersionCommand command = UploadNewVersionCommand.builder()
                .documentId(id)
                .fileContent(fileBytes)
                .mimeType(file.getContentType())
                .uploadedBy(currentUserId)
                .changeSummary(changeSummary)
                .build();

        DocumentVersionResponseDto versionDto = uploadNewVersionUseCase.uploadNewVersion(command);
        return ResponseEntity.created(URI.create("/api/v1/documents/" + id + "/versions/" + versionDto.getId())).body(versionDto);
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAuthority('DOCUMENT_READ') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER') or hasRole('VIEWER')")
    public ResponseEntity<List<DocumentVersionResponseDto>> listVersions(@PathVariable("id") UUID id) {
        List<DocumentVersionResponseDto> versions = listDocumentVersionsUseCase.listVersions(id);
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/{id}/versions/{versionId}/download")
    @PreAuthorize("hasAuthority('DOCUMENT_READ') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER') or hasRole('VIEWER')")
    public ResponseEntity<StreamingResponseBody> downloadSpecificVersion(
            @PathVariable("id") UUID id,
            @PathVariable("versionId") UUID versionId) {
        DownloadDocumentUseCase.DownloadResult result = downloadDocumentUseCase.download(id, versionId);
        return buildStreamingDownloadResponse(result);
    }

    // =========================================================================
    // 5. CHECKOUT / LOCKING
    // =========================================================================

    @PostMapping("/{id}/checkout")
    @PreAuthorize("hasAuthority('DOCUMENT_WRITE') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<DocumentLockResponseDto> checkoutDocument(@PathVariable("id") UUID id) {
        UUID currentUserId = resolveCurrentUserId();
        DocumentLockResponseDto lock = checkoutDocumentUseCase.checkout(id, currentUserId);
        return ResponseEntity.ok(lock);
    }

    @PostMapping("/{id}/checkin")
    @PreAuthorize("hasAuthority('DOCUMENT_WRITE') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<Void> checkinDocument(@PathVariable("id") UUID id) {
        UUID currentUserId = resolveCurrentUserId();
        checkinDocumentUseCase.checkin(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/lock")
    @PreAuthorize("hasAuthority('DOCUMENT_READ') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER') or hasRole('VIEWER')")
    public ResponseEntity<DocumentLockResponseDto> getLockStatus(@PathVariable("id") UUID id) {
        DocumentLockResponseDto lock = getDocumentLockUseCase.getLockStatus(id);
        return ResponseEntity.ok(lock);
    }

    // =========================================================================
    // 6. RENAME / MOVE / STATUS UPDATE / DELETE
    // =========================================================================

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENT_WRITE') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<DocumentResponseDto> updateDocument(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateDocumentCommand command) {
        UUID currentUserId = resolveCurrentUserId();
        DocumentResponseDto updated = updateDocumentUseCase.updateDocument(id, command, currentUserId);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('DOCUMENT_WRITE') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<DocumentResponseDto> updateStatus(
            @PathVariable("id") UUID id,
            @RequestParam("status") String status) {
        UUID currentUserId = resolveCurrentUserId();
        DocumentResponseDto updated = updateDocumentStatusUseCase.updateStatus(id, status, currentUserId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENT_DELETE') or hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Void> deleteDocument(@PathVariable("id") UUID id) {
        UUID currentUserId = resolveCurrentUserId();
        deleteDocumentUseCase.deleteDocument(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('DOCUMENT_WRITE') or hasRole('TRASH_RESTORE') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<DocumentResponseDto> restoreDocument(@PathVariable("id") UUID id) {
        UUID currentUserId = resolveCurrentUserId();
        DocumentResponseDto restored = updateDocumentStatusUseCase.updateStatus(id, "DRAFT", currentUserId);
        return ResponseEntity.ok(restored);
    }

    @GetMapping("/trash")
    @PreAuthorize("hasAuthority('DOCUMENT_READ') or hasRole('TRASH_READ') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<com.awb.ged.common.model.PageResponse<DocumentSearchResultDto>> getDocumentTrash(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        DocumentSearchQuery query = DocumentSearchQuery.builder()
                .status("TRASHED")
                .page(page)
                .size(size)
                .sortBy("deletedAt")
                .sortDirection("DESC")
                .build();
        com.awb.ged.common.model.PageResponse<DocumentSearchResultDto> results = searchDocumentsUseCase.search(query);
        return ResponseEntity.ok(results);
    }

    // =========================================================================
    // 7. BULK ACTIONS
    // =========================================================================

    @DeleteMapping("/bulk")
    @PreAuthorize("hasAuthority('DOCUMENT_DELETE') or hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Void> bulkDelete(@RequestBody List<UUID> documentIds) {
        UUID currentUserId = resolveCurrentUserId();
        bulkDocumentActionUseCase.bulkDelete(documentIds, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/bulk/move")
    @PreAuthorize("hasAuthority('DOCUMENT_WRITE') or hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Void> bulkMove(
            @RequestBody List<UUID> documentIds,
            @RequestParam(value = "targetFolderId", required = false) UUID targetFolderId,
            @RequestParam(value = "moveToRoot", defaultValue = "false") boolean moveToRoot) {
        UUID currentUserId = resolveCurrentUserId();
        bulkDocumentActionUseCase.bulkMove(documentIds, targetFolderId, moveToRoot, currentUserId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bulk/tag")
    @PreAuthorize("hasAuthority('DOCUMENT_WRITE') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<Void> bulkTag(
            @RequestBody List<UUID> documentIds,
            @RequestParam("tagNames") List<String> tagNames) {
        UUID currentUserId = resolveCurrentUserId();
        bulkDocumentActionUseCase.bulkTag(documentIds, tagNames, currentUserId);
        return ResponseEntity.ok().build();
    }

    // =========================================================================
    // 8. PERMISSIONS
    // =========================================================================

    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('DOCUMENT_WRITE') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<PermissionResponseDto> grantPermission(
            @PathVariable("id") UUID id,
            @Valid @RequestBody GrantPermissionRequest request) {
        UUID currentUserId = resolveCurrentUserId();
        boolean isAdminOrManager = checkIsAdminOrManager();

        GrantPermissionCommand command = GrantPermissionCommand.builder()
                .targetId(id)
                .userId(request.getUserId())
                .groupId(request.getGroupId())
                .canRead(request.isCanRead())
                .canWrite(request.isCanWrite())
                .canDelete(request.isCanDelete())
                .canShareOrManage(request.isCanShareOrManage())
                .grantedBy(currentUserId)
                .build();

        PermissionResponseDto granted = grantDocumentPermissionUseCase.grantPermission(command, isAdminOrManager);
        return ResponseEntity.created(URI.create("/api/v1/documents/" + id + "/permissions/" + granted.getId())).body(granted);
    }

    @DeleteMapping("/{id}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('DOCUMENT_WRITE') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<Void> revokePermission(
            @PathVariable("id") UUID id,
            @PathVariable("permissionId") UUID permissionId) {
        UUID currentUserId = resolveCurrentUserId();
        boolean isAdminOrManager = checkIsAdminOrManager();
        revokeDocumentPermissionUseCase.revokePermission(id, permissionId, currentUserId, isAdminOrManager);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('DOCUMENT_READ') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<List<PermissionResponseDto>> listPermissions(@PathVariable("id") UUID id) {
        UUID currentUserId = resolveCurrentUserId();
        boolean isAdminOrManager = checkIsAdminOrManager();
        List<PermissionResponseDto> list = listDocumentPermissionsUseCase.listPermissions(id, currentUserId, isAdminOrManager);
        return ResponseEntity.ok(list);
    }

    // =========================================================================
    // PRIVATE HELPER METHODS
    // =========================================================================

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException(ErrorCode.INVALID_INPUT, "Uploaded file cannot be empty.");
        }

        if (file.getSize() > maxFileSize) {
            throw new MaxUploadSizeExceededException(maxFileSize);
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase())) {
            throw new InvalidRequestException(
                    ErrorCode.INVALID_DOCUMENT_FORMAT,
                    "Unsupported MIME type '" + mimeType + "'. Allowed types: " + ALLOWED_MIME_TYPES
            );
        }
    }

    private byte[] extractBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new InvalidRequestException(ErrorCode.INVALID_INPUT, "Failed to read file content: " + e.getMessage());
        }
    }

    private ResponseEntity<StreamingResponseBody> buildStreamingDownloadResponse(DownloadDocumentUseCase.DownloadResult result) {
        StreamingResponseBody body = outputStream -> {
            try (InputStream in = result.inputStream()) {
                in.transferTo(outputStream);
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(result.fileName()).build().toString())
                .contentLength(result.sizeBytes())
                .body(body);
    }

    private UUID resolveCurrentUserId() {
        CurrentUser currentUser = currentUserProvider.getRequiredCurrentUser();
        return userRepositoryPort.findByKeycloakSub(currentUser.getKeycloakSub())
                .map(User::getId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .id(UUID.randomUUID())
                            .keycloakSub(currentUser.getKeycloakSub())
                            .username(currentUser.getUsername() != null ? currentUser.getUsername() : "user_" + currentUser.getKeycloakSub().substring(0, Math.min(8, currentUser.getKeycloakSub().length())))
                            .email(currentUser.getEmail() != null ? currentUser.getEmail() : currentUser.getUsername() + "@awb.ma")
                            .firstName(currentUser.getUsername() != null ? currentUser.getUsername() : "User")
                            .lastName("GED")
                            .active(true)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    try {
                        return userRepositoryPort.save(newUser).getId();
                    } catch (DataIntegrityViolationException e) {
                        return userRepositoryPort.findByKeycloakSub(currentUser.getKeycloakSub())
                                .map(User::getId)
                                .orElseThrow(() -> e);
                    }
                });
    }

    private boolean checkIsAdminOrManager() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));
    }
}
