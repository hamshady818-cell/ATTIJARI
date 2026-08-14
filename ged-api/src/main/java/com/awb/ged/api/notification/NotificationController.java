package com.awb.ged.api.notification;

import com.awb.ged.application.dto.notification.NotificationResponseDto;
import com.awb.ged.application.port.in.notification.GetNotificationsUseCase;
import com.awb.ged.application.port.in.notification.MarkAllNotificationsReadUseCase;
import com.awb.ged.application.port.in.notification.MarkNotificationReadUseCase;
import com.awb.ged.application.port.in.security.CurrentUserProvider;
import com.awb.ged.application.port.out.persistence.UserRepositoryPort;
import com.awb.ged.common.security.CurrentUser;
import com.awb.ged.domain.user.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final GetNotificationsUseCase getNotificationsUseCase;
    private final MarkNotificationReadUseCase markNotificationReadUseCase;
    private final MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase;
    private final CurrentUserProvider currentUserProvider;
    private final UserRepositoryPort userRepositoryPort;

    @Autowired
    public NotificationController(GetNotificationsUseCase getNotificationsUseCase,
                                  MarkNotificationReadUseCase markNotificationReadUseCase,
                                  MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase,
                                  CurrentUserProvider currentUserProvider,
                                  UserRepositoryPort userRepositoryPort) {
        this.getNotificationsUseCase = getNotificationsUseCase;
        this.markNotificationReadUseCase = markNotificationReadUseCase;
        this.markAllNotificationsReadUseCase = markAllNotificationsReadUseCase;
        this.currentUserProvider = currentUserProvider;
        this.userRepositoryPort = userRepositoryPort;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('NOTIFICATION_READ') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<List<NotificationResponseDto>> getNotifications(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        UUID userId = resolveCurrentUserId();
        List<NotificationResponseDto> list = getNotificationsUseCase.getNotifications(userId, page, size);
        return ResponseEntity.ok(list);
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAuthority('NOTIFICATION_WRITE') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<Void> markAsRead(@PathVariable("id") UUID id) {
        UUID userId = resolveCurrentUserId();
        markNotificationReadUseCase.markAsRead(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasAuthority('NOTIFICATION_WRITE') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<Void> markAllAsRead() {
        UUID userId = resolveCurrentUserId();
        markAllNotificationsReadUseCase.markAllAsRead(userId);
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
}
