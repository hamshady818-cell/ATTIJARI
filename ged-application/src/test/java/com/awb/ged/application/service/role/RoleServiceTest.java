package com.awb.ged.application.service.role;

import com.awb.ged.application.dto.role.RoleResponseDto;
import com.awb.ged.application.mapper.RoleMapper;
import com.awb.ged.application.port.out.persistence.RoleRepositoryPort;
import com.awb.ged.domain.role.model.Role;
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepositoryPort roleRepositoryPort;

    private final RoleMapper roleMapper = Mappers.getMapper(RoleMapper.class);

    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleService = new RoleService(roleRepositoryPort, roleMapper);
    }

    @Test
    @DisplayName("Should list all roles successfully")
    void listRoles_Success() {
        // Given
        UUID roleId = UUID.randomUUID();
        Role role = Role.builder()
                .id(roleId)
                .name("ADMIN")
                .description("Administrator")
                .build();

        given(roleRepositoryPort.findAll()).willReturn(List.of(role));

        // When
        List<RoleResponseDto> result = roleService.listRoles();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("ADMIN");
        assertThat(result.get(0).getDescription()).isEqualTo("Administrator");
        verify(roleRepositoryPort).findAll();
    }
}
