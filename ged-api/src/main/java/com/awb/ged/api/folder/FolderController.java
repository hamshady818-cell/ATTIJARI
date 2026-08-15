package com.awb.ged.api.folder;

import com.awb.ged.api.folder.dto.CreateFolderRequest;
import com.awb.ged.api.permission.dto.GrantPermissionRequest;
import com.awb.ged.application.dto.folder.CreateFolderCommand;
import com.awb.ged.application.dto.folder.FolderContentResponseDto;
import com.awb.ged.application.dto.folder.FolderResponseDto;
import com.awb.ged.application.dto.permission.GrantPermissionCommand;
import com.awb.ged.application.dto.permission.PermissionResponseDto;
import com.awb.ged.application.port.in.folder.CreateFolderUseCase;
import com.awb.ged.application.port.in.folder.DeleteFolderUseCase;
import com.awb.ged.application.port.in.folder.GetFolderContentUseCase;
import com.awb.ged.application.port.in.folder.GrantFolderPermissionUseCase;
import com.awb.ged.application.port.in.folder.RevokeFolderPermissionUseCase;
import com.awb.ged.application.port.in.folder.ListFolderPermissionsUseCase;
import com.awb.ged.application.port.in.security.CurrentUserProvider;
import com.awb.ged.application.port.out.persistence.UserRepositoryPort;
import com.awb.ged.common.security.CurrentUser;
import com.awb.ged.domain.user.model.User;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.awb.ged.application.port.in.folder.GetAllFoldersUseCase;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/folders")
public class FolderController {

    private final CreateFolderUseCase createFolderUseCase;
    private final GetFolderContentUseCase getFolderContentUseCase;
    private final GetAllFoldersUseCase getAllFoldersUseCase;
    private final DeleteFolderUseCase deleteFolderUseCase;
    private final GrantFolderPermissionUseCase grantFolderPermissionUseCase;
    private final RevokeFolderPermissionUseCase revokeFolderPermissionUseCase;
    private final ListFolderPermissionsUseCase listFolderPermissionsUseCase;
    private final CurrentUserProvider currentUserProvider;
    private final UserRepositoryPort userRepositoryPort;

    public FolderController(CreateFolderUseCase createFolderUseCase,
                            GetFolderContentUseCase getFolderContentUseCase,
                            GetAllFoldersUseCase getAllFoldersUseCase,
                            DeleteFolderUseCase deleteFolderUseCase,
                            GrantFolderPermissionUseCase grantFolderPermissionUseCase,
                            RevokeFolderPermissionUseCase revokeFolderPermissionUseCase,
                            ListFolderPermissionsUseCase listFolderPermissionsUseCase,
                            CurrentUserProvider currentUserProvider,
                            UserRepositoryPort userRepositoryPort) {
        this.createFolderUseCase = createFolderUseCase;
        this.getFolderContentUseCase = getFolderContentUseCase;
        this.getAllFoldersUseCase = getAllFoldersUseCase;
        this.deleteFolderUseCase = deleteFolderUseCase;
        this.grantFolderPermissionUseCase = grantFolderPermissionUseCase;
        this.revokeFolderPermissionUseCase = revokeFolderPermissionUseCase;
        this.listFolderPermissionsUseCase = listFolderPermissionsUseCase;
        this.currentUserProvider = currentUserProvider;
        this.userRepositoryPort = userRepositoryPort;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FolderResponseDto>> getAllFolders() {
        List<FolderResponseDto> folders = getAllFoldersUseCase.getAllFolders();
        return ResponseEntity.ok(folders);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FolderResponseDto> createFolder(@Valid @RequestBody CreateFolderRequest request) {
        UUID ownerId = resolveCurrentUserId();

        CreateFolderCommand command = CreateFolderCommand.builder()
                .name(request.getName())
                .parentFolderId(request.getParentFolderId())
                .ownerId(ownerId)
                .build();

        FolderResponseDto created = createFolderUseCase.createFolder(command);
        URI location = URI.create("/api/v1/folders/" + created.getId());

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}/content")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FolderContentResponseDto> getFolderContent(@PathVariable("id") UUID id) {
        FolderContentResponseDto content = getFolderContentUseCase.getFolderContent(id);
        return ResponseEntity.ok(content);
    }

    @GetMapping("/content")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FolderContentResponseDto> getRootContent() {
        FolderContentResponseDto content = getFolderContentUseCase.getFolderContent(null);
        return ResponseEntity.ok(content);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FOLDER_DELETE') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<Void> deleteFolder(
            @PathVariable("id") UUID id,
            @RequestParam(name = "force", defaultValue = "false") boolean force) {
        UUID deletedBy = resolveCurrentUserId();
        deleteFolderUseCase.deleteFolder(id, deletedBy, force);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveCurrentUserId() {
        CurrentUser currentUser = currentUserProvider.getRequiredCurrentUser();
        return userRepositoryPort.findByKeycloakSub(currentUser.getKeycloakSub())
                .map(User::getId)
                .orElseGet(() -> {
                    String sub = currentUser.getKeycloakSub() != null && !currentUser.getKeycloakSub().isBlank()
                            ? currentUser.getKeycloakSub()
                            : UUID.randomUUID().toString();
                    String username = currentUser.getUsername() != null && !currentUser.getUsername().isBlank() && !"unknown".equalsIgnoreCase(currentUser.getUsername())
                            ? currentUser.getUsername()
                            : "user_" + sub.substring(0, Math.min(8, sub.length())).replaceAll("[^a-zA-Z0-9_]", "");
                    String rawEmail = currentUser.getEmail();
                    String email = (rawEmail != null && !rawEmail.isBlank()) ? rawEmail : username + "@awb.ma";

                    User newUser = User.builder()
                            .id(UUID.randomUUID())
                            .keycloakSub(sub)
                            .username(username)
                            .email(email)
                            .firstName(username)
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

    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('FOLDER_WRITE') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
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

        PermissionResponseDto granted = grantFolderPermissionUseCase.grantPermission(command, isAdminOrManager);
        return ResponseEntity.created(URI.create("/api/v1/folders/" + id + "/permissions/" + granted.getId())).body(granted);
    }

    @DeleteMapping("/{id}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('FOLDER_WRITE') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<Void> revokePermission(
            @PathVariable("id") UUID id,
            @PathVariable("permissionId") UUID permissionId) {
        UUID currentUserId = resolveCurrentUserId();
        boolean isAdminOrManager = checkIsAdminOrManager();
        revokeFolderPermissionUseCase.revokePermission(id, permissionId, currentUserId, isAdminOrManager);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('FOLDER_READ') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<List<PermissionResponseDto>> listPermissions(
            @PathVariable("id") UUID id) {
        UUID currentUserId = resolveCurrentUserId();
        boolean isAdminOrManager = checkIsAdminOrManager();
        List<PermissionResponseDto> list = listFolderPermissionsUseCase.listPermissions(id, currentUserId, isAdminOrManager);
        return ResponseEntity.ok(list);
    }

    private boolean checkIsAdminOrManager() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));
    }
}
