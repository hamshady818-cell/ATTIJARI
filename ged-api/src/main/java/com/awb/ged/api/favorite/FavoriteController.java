package com.awb.ged.api.favorite;

import com.awb.ged.api.favorite.dto.AddFavoriteRequest;
import com.awb.ged.application.dto.favorite.AddFavoriteCommand;
import com.awb.ged.application.dto.favorite.FavoriteResponseDto;
import com.awb.ged.application.port.in.favorite.AddFavoriteUseCase;
import com.awb.ged.application.port.in.favorite.GetFavoritesUseCase;
import com.awb.ged.application.port.in.favorite.RemoveFavoriteUseCase;
import com.awb.ged.application.port.in.security.CurrentUserProvider;
import com.awb.ged.application.port.out.persistence.UserRepositoryPort;
import com.awb.ged.common.security.CurrentUser;
import com.awb.ged.domain.user.model.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/favorites")
public class FavoriteController {

    private final AddFavoriteUseCase addFavoriteUseCase;
    private final RemoveFavoriteUseCase removeFavoriteUseCase;
    private final GetFavoritesUseCase getFavoritesUseCase;
    private final CurrentUserProvider currentUserProvider;
    private final UserRepositoryPort userRepositoryPort;

    @Autowired
    public FavoriteController(AddFavoriteUseCase addFavoriteUseCase,
                              RemoveFavoriteUseCase removeFavoriteUseCase,
                              GetFavoritesUseCase getFavoritesUseCase,
                              CurrentUserProvider currentUserProvider,
                              UserRepositoryPort userRepositoryPort) {
        this.addFavoriteUseCase = addFavoriteUseCase;
        this.removeFavoriteUseCase = removeFavoriteUseCase;
        this.getFavoritesUseCase = getFavoritesUseCase;
        this.currentUserProvider = currentUserProvider;
        this.userRepositoryPort = userRepositoryPort;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FAVORITE_WRITE') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<FavoriteResponseDto> addFavorite(@Valid @RequestBody AddFavoriteRequest request) {
        UUID userId = resolveCurrentUserId();

        AddFavoriteCommand command = AddFavoriteCommand.builder()
                .userId(userId)
                .entityType(request.getEntityType())
                .entityId(request.getEntityId())
                .build();

        FavoriteResponseDto created = addFavoriteUseCase.addFavorite(command);
        URI location = URI.create("/api/v1/favorites/" + created.getId());

        return ResponseEntity.created(location).body(created);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FAVORITE_WRITE') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<Void> removeFavorite(@PathVariable("id") UUID id) {
        UUID userId = resolveCurrentUserId();
        removeFavoriteUseCase.removeFavorite(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FAVORITE_READ') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<List<FavoriteResponseDto>> getFavorites() {
        UUID userId = resolveCurrentUserId();
        List<FavoriteResponseDto> favorites = getFavoritesUseCase.getFavorites(userId);
        return ResponseEntity.ok(favorites);
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
