package com.awb.ged.api.favorite;

import com.awb.ged.api.exception.GlobalExceptionHandler;
import com.awb.ged.api.favorite.dto.AddFavoriteRequest;
import com.awb.ged.application.dto.favorite.FavoriteResponseDto;
import com.awb.ged.application.port.in.favorite.AddFavoriteUseCase;
import com.awb.ged.application.port.in.favorite.GetFavoritesUseCase;
import com.awb.ged.application.port.in.favorite.RemoveFavoriteUseCase;
import com.awb.ged.application.port.in.security.CurrentUserProvider;
import com.awb.ged.application.port.out.persistence.UserRepositoryPort;
import com.awb.ged.common.exception.ConflictException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.ForbiddenException;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.common.security.CurrentUser;
import com.awb.ged.domain.user.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FavoriteController.class)
@Import({GlobalExceptionHandler.class, FavoriteControllerTest.MethodSecurityConfig.class})
class FavoriteControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AddFavoriteUseCase addFavoriteUseCase;

    @MockitoBean
    private RemoveFavoriteUseCase removeFavoriteUseCase;

    @MockitoBean
    private GetFavoritesUseCase getFavoritesUseCase;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @MockitoBean
    private UserRepositoryPort userRepositoryPort;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        CurrentUser currentUser = CurrentUser.builder()
                .keycloakSub("sub-fav")
                .username("favuser")
                .email("favuser@awb.ma")
                .build();

        given(currentUserProvider.getRequiredCurrentUser()).willReturn(currentUser);
        given(userRepositoryPort.findByKeycloakSub("sub-fav"))
                .willReturn(Optional.of(User.builder().id(userId).keycloakSub("sub-fav").build()));
    }

    @Test
    @WithMockUser(authorities = "FAVORITE_WRITE")
    @DisplayName("POST /api/v1/favorites - Should add favorite and return 201 Created")
    void addFavorite_Success() throws Exception {
        // Given
        UUID favoriteId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        AddFavoriteRequest request = AddFavoriteRequest.builder()
                .entityType("DOCUMENT")
                .entityId(docId)
                .build();

        FavoriteResponseDto responseDto = FavoriteResponseDto.builder()
                .id(favoriteId)
                .userId(userId)
                .entityType("DOCUMENT")
                .entityId(docId)
                .createdAt(Instant.now())
                .build();

        given(addFavoriteUseCase.addFavorite(any())).willReturn(responseDto);

        // When / Then
        mockMvc.perform(post("/api/v1/favorites")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/favorites/" + favoriteId))
                .andExpect(jsonPath("$.id").value(favoriteId.toString()))
                .andExpect(jsonPath("$.entityType").value("DOCUMENT"));
    }

    @Test
    @WithMockUser(authorities = "FAVORITE_WRITE")
    @DisplayName("POST /api/v1/favorites - Should return 400 Bad Request on empty entityType")
    void addFavorite_EmptyType_Returns400() throws Exception {
        // Given
        AddFavoriteRequest request = AddFavoriteRequest.builder()
                .entityType("   ")
                .entityId(UUID.randomUUID())
                .build();

        // When / Then
        mockMvc.perform(post("/api/v1/favorites")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ERR-SYS-002"));
    }

    @Test
    @WithMockUser(authorities = "FAVORITE_WRITE")
    @DisplayName("POST /api/v1/favorites - Should return 409 Conflict when item already favorited")
    void addFavorite_Duplicate_Returns409() throws Exception {
        // Given
        AddFavoriteRequest request = AddFavoriteRequest.builder()
                .entityType("DOCUMENT")
                .entityId(UUID.randomUUID())
                .build();

        given(addFavoriteUseCase.addFavorite(any()))
                .willThrow(new ConflictException(ErrorCode.INVALID_INPUT, "Already favorited"));

        // When / Then
        mockMvc.perform(post("/api/v1/favorites")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ERR-SYS-002"));
    }

    @Test
    @WithMockUser(authorities = "FAVORITE_WRITE")
    @DisplayName("DELETE /api/v1/favorites/{id} - Should remove favorite and return 204 No Content")
    void removeFavorite_Success() throws Exception {
        // Given
        UUID favoriteId = UUID.randomUUID();
        doNothing().when(removeFavoriteUseCase).removeFavorite(favoriteId, userId);

        // When / Then
        mockMvc.perform(delete("/api/v1/favorites/{id}", favoriteId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "FAVORITE_WRITE")
    @DisplayName("DELETE /api/v1/favorites/{id} - Should return 403 when not owned by user")
    void removeFavorite_NotOwned_Returns403() throws Exception {
        // Given
        UUID favoriteId = UUID.randomUUID();
        doThrow(new ForbiddenException(ErrorCode.FORBIDDEN, "Not owned"))
                .when(removeFavoriteUseCase).removeFavorite(favoriteId, userId);

        // When / Then
        mockMvc.perform(delete("/api/v1/favorites/{id}", favoriteId)
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ERR-SEC-002"));
    }

    @Test
    @WithMockUser(authorities = "FAVORITE_READ")
    @DisplayName("GET /api/v1/favorites - Should retrieve user favorites and return 200 OK")
    void getFavorites_Success() throws Exception {
        // Given
        FavoriteResponseDto f = FavoriteResponseDto.builder()
                .id(UUID.randomUUID())
                .entityType("DOCUMENT")
                .entityId(UUID.randomUUID())
                .build();

        given(getFavoritesUseCase.getFavorites(userId)).willReturn(List.of(f));

        // When / Then
        mockMvc.perform(get("/api/v1/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].entityType").value("DOCUMENT"));
    }
}
