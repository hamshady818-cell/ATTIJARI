package com.awb.ged.api.document;

import com.awb.ged.api.permission.dto.GrantPermissionRequest;
import com.awb.ged.application.dto.document.DocumentResponseDto;
import com.awb.ged.application.dto.document.UploadDocumentCommand;
import com.awb.ged.application.dto.permission.GrantPermissionCommand;
import com.awb.ged.application.dto.permission.PermissionResponseDto;
import com.awb.ged.application.port.in.document.DeleteDocumentUseCase;
import com.awb.ged.application.port.in.document.GetDocumentUseCase;
import com.awb.ged.application.port.in.document.UploadDocumentUseCase;
import com.awb.ged.application.port.in.document.GrantDocumentPermissionUseCase;
import com.awb.ged.application.port.in.document.RevokeDocumentPermissionUseCase;
import com.awb.ged.application.port.in.document.ListDocumentPermissionsUseCase;
import com.awb.ged.application.port.in.security.CurrentUserProvider;
import com.awb.ged.application.port.out.persistence.UserRepositoryPort;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.InvalidRequestException;
import com.awb.ged.common.security.CurrentUser;
import com.awb.ged.domain.user.model.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.net.URI;
import java.time.Instant;
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
            "image/tiff"
    );

    private final UploadDocumentUseCase uploadDocumentUseCase;
    private final GetDocumentUseCase getDocumentUseCase;
    private final CurrentUserProvider currentUserProvider;
    private final UserRepositoryPort userRepositoryPort;
    private final long maxFileSize;
    private final DeleteDocumentUseCase deleteDocumentUseCase;
    private final GrantDocumentPermissionUseCase grantDocumentPermissionUseCase;
    private final RevokeDocumentPermissionUseCase revokeDocumentPermissionUseCase;
    private final ListDocumentPermissionsUseCase listDocumentPermissionsUseCase;

    public DocumentController(UploadDocumentUseCase uploadDocumentUseCase,
                              GetDocumentUseCase getDocumentUseCase,
                              DeleteDocumentUseCase deleteDocumentUseCase,
                              GrantDocumentPermissionUseCase grantDocumentPermissionUseCase,
                              RevokeDocumentPermissionUseCase revokeDocumentPermissionUseCase,
                              ListDocumentPermissionsUseCase listDocumentPermissionsUseCase,
                              CurrentUserProvider currentUserProvider,
                              UserRepositoryPort userRepositoryPort,
                              @Value("${ged.upload.max-file-size:52428800}") long maxFileSize) {
        this.uploadDocumentUseCase = uploadDocumentUseCase;
        this.getDocumentUseCase = getDocumentUseCase;
        this.deleteDocumentUseCase = deleteDocumentUseCase;
        this.grantDocumentPermissionUseCase = grantDocumentPermissionUseCase;
        this.revokeDocumentPermissionUseCase = revokeDocumentPermissionUseCase;
        this.listDocumentPermissionsUseCase = listDocumentPermissionsUseCase;
        this.currentUserProvider = currentUserProvider;
        this.userRepositoryPort = userRepositoryPort;
        this.maxFileSize = maxFileSize;
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('DOCUMENT_CREATE') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<DocumentResponseDto> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam(value = "folderId", required = false) UUID folderId,
            @RequestParam(value = "categoryId", required = false) UUID categoryId) {

        // 1. File Validation
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

        if (name == null || name.trim().isBlank()) {
            throw new InvalidRequestException(ErrorCode.INVALID_INPUT, "Document name is required.");
        }

        // 2. Resolve owner ID from authenticated JWT user context
        UUID ownerId = resolveCurrentUserId();

        // 3. Extract file bytes
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new InvalidRequestException(ErrorCode.INVALID_INPUT, "Failed to read uploaded file content: " + e.getMessage());
        }

        // 4. Construct Command and invoke UseCase
        UploadDocumentCommand command = UploadDocumentCommand.builder()
                .name(name.trim())
                .folderId(folderId)
                .categoryId(categoryId)
                .ownerId(ownerId)
                .mimeType(mimeType)
                .fileContent(fileBytes)
                .build();

        DocumentResponseDto created = uploadDocumentUseCase.uploadDocument(command);
        URI location = URI.create("/api/v1/documents/" + created.getId());

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENT_READ') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER') or hasRole('VIEWER')")
    public ResponseEntity<DocumentResponseDto> getDocumentById(@PathVariable("id") UUID id) {
        DocumentResponseDto document = getDocumentUseCase.getDocumentById(id);
        return ResponseEntity.ok(document);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENT_DELETE') or hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Void> deleteDocument(@PathVariable("id") UUID id) {
        UUID currentUserId = resolveCurrentUserId();
        deleteDocumentUseCase.deleteDocument(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveCurrentUserId() {
        CurrentUser currentUser = currentUserProvider.getRequiredCurrentUser();
        return userRepositoryPort.findByKeycloakSub(currentUser.getKeycloakSub())
                .map(User::getId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .id(UUID.randomUUID())
                            .keycloakSub(currentUser.getKeycloakSub())
                            .username(currentUser.getUsername() != null ? currentUser.getUsername() : "user_" + currentUser.getKeycloakSub().substring(0, 8))
                            .email(currentUser.getEmail() != null ? currentUser.getEmail() : currentUser.getUsername() + "@awb.ma")
                            .firstName(currentUser.getUsername() != null ? currentUser.getUsername() : "User")
                            .lastName("GED")
                            .active(true)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    return userRepositoryPort.save(newUser).getId();
                });
    }

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
    public ResponseEntity<List<PermissionResponseDto>> listPermissions(
            @PathVariable("id") UUID id) {
        UUID currentUserId = resolveCurrentUserId();
        boolean isAdminOrManager = checkIsAdminOrManager();
        List<PermissionResponseDto> list = listDocumentPermissionsUseCase.listPermissions(id, currentUserId, isAdminOrManager);
        return ResponseEntity.ok(list);
    }

    private boolean checkIsAdminOrManager() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));
    }
}
