package com.awb.ged.application.service.trash;

import com.awb.ged.application.dto.trash.TrashItemResponseDto;
import com.awb.ged.application.mapper.TrashMapper;
import com.awb.ged.application.port.out.persistence.TrashRepositoryPort;
import com.awb.ged.common.model.PageResponse;
import com.awb.ged.domain.trash.model.TrashItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GetTrashServiceTest {

    @Mock
    private TrashRepositoryPort trashRepositoryPort;

    private final TrashMapper trashMapper = Mappers.getMapper(TrashMapper.class);

    private GetTrashService getTrashService;

    @BeforeEach
    void setUp() {
        getTrashService = new GetTrashService(trashRepositoryPort, trashMapper);
    }

    @Test
    @DisplayName("Should retrieve personal trash page when user is regular user")
    void getTrash_RegularUser() {
        // Given
        UUID userId = UUID.randomUUID();
        TrashItem item1 = TrashItem.builder().id(UUID.randomUUID()).entityType("DOCUMENT").name("Doc1.pdf").deletedBy(userId).build();

        PageResponse<TrashItem> domainPage = PageResponse.<TrashItem>builder()
                .content(List.of(item1))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .empty(false)
                .build();

        given(trashRepositoryPort.findTrash(eq(userId), eq(false), eq(0), eq(10))).willReturn(domainPage);

        // When
        PageResponse<TrashItemResponseDto> result = getTrashService.getTrash(userId, false, 0, 10);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeletedBy()).isEqualTo(userId);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Doc1.pdf");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should sanitize invalid page and size parameters")
    void getTrash_SanitizesParameters() {
        // Given
        UUID userId = UUID.randomUUID();
        PageResponse<TrashItem> domainPage = PageResponse.<TrashItem>builder()
                .content(List.of())
                .pageNumber(0)
                .pageSize(100)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .empty(true)
                .build();

        given(trashRepositoryPort.findTrash(eq(userId), eq(true), eq(0), eq(100))).willReturn(domainPage);

        // When (passing negative page and size > 100)
        PageResponse<TrashItemResponseDto> result = getTrashService.getTrash(userId, true, -1, 500);

        // Then
        assertThat(result.isEmpty()).isTrue();
    }
}
