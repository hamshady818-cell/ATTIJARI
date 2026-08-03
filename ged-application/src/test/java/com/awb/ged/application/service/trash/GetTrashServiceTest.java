package com.awb.ged.application.service.trash;

import com.awb.ged.application.dto.trash.TrashItemResponseDto;
import com.awb.ged.application.mapper.TrashMapper;
import com.awb.ged.application.port.out.persistence.TrashRepositoryPort;
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
    @DisplayName("Should retrieve personal trash list when user is regular user")
    void getTrash_RegularUser() {
        // Given
        UUID userId = UUID.randomUUID();
        TrashItem item1 = TrashItem.builder().id(UUID.randomUUID()).entityType("DOCUMENT").deletedBy(userId).build();

        given(trashRepositoryPort.findByDeletedByAndPurgedAtIsNull(userId)).willReturn(List.of(item1));

        // When
        List<TrashItemResponseDto> result = getTrashService.getTrash(userId, false);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeletedBy()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should retrieve all trash list when user is admin or manager")
    void getTrash_AdminUser() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        TrashItem item1 = TrashItem.builder().id(UUID.randomUUID()).entityType("DOCUMENT").deletedBy(userId).build();
        TrashItem item2 = TrashItem.builder().id(UUID.randomUUID()).entityType("FOLDER").deletedBy(otherUserId).build();

        given(trashRepositoryPort.findAllActive()).willReturn(List.of(item1, item2));

        // When
        List<TrashItemResponseDto> result = getTrashService.getTrash(userId, true);

        // Then
        assertThat(result).hasSize(2);
    }
}
