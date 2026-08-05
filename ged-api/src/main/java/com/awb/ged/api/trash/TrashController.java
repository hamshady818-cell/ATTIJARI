package com.awb.ged.api.trash;

import com.awb.ged.application.dto.trash.TrashItemResponseDto;
import com.awb.ged.application.port.in.security.CurrentUserProvider;
import com.awb.ged.application.port.in.trash.GetTrashUseCase;
import com.awb.ged.application.port.in.trash.RestoreFromTrashUseCase;
import com.awb.ged.application.port.out.persistence.UserRepositoryPort;
import com.awb.ged.common.security.CurrentUser;
import com.awb.ged.domain.user.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trash")
public class TrashController {

    private final GetTrashUseCase getTrashUseCase;
    private final RestoreFromTrashUseCase restoreFromTrashUseCase;
    private final CurrentUserProvider currentUserProvider;
    private final UserRepositoryPort userRepositoryPort;

    @Autowired
    public TrashController(GetTrashUseCase getTrashUseCase,
                           RestoreFromTrashUseCase restoreFromTrashUseCase,
                           CurrentUserProvider currentUserProvider,
                           UserRepositoryPort userRepositoryPort) {
        this.getTrashUseCase = getTrashUseCase;
        this.restoreFromTrashUseCase = restoreFromTrashUseCase;
        this.currentUserProvider = currentUserProvider;
        this.userRepositoryPort = userRepositoryPort;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TRASH_READ') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<List<TrashItemResponseDto>> getTrash() {
        UUID userId = resolveCurrentUserId();
        boolean isAdminOrManager = checkIsAdminOrManager();

        List<TrashItemResponseDto> trash = getTrashUseCase.getTrash(userId, isAdminOrManager);
        return ResponseEntity.ok(trash);
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('TRASH_RESTORE') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<Void> restoreFromTrash(@PathVariable("id") UUID id) {
        UUID userId = resolveCurrentUserId();
        restoreFromTrashUseCase.restoreFromTrash(id, userId);
        return ResponseEntity.noContent().build();
    }

    private boolean checkIsAdminOrManager() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));
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
                    return userRepositoryPort.save(newUser).getId();
                });
    }
}
