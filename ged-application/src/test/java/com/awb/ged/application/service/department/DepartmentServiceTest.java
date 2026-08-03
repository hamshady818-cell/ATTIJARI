package com.awb.ged.application.service.department;

import com.awb.ged.application.dto.department.CreateDepartmentCommand;
import com.awb.ged.application.dto.department.DepartmentResponseDto;
import com.awb.ged.application.dto.department.UpdateDepartmentCommand;
import com.awb.ged.application.mapper.DepartmentMapper;
import com.awb.ged.application.port.out.persistence.DepartmentRepositoryPort;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.department.model.Department;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepositoryPort departmentRepositoryPort;

    private final DepartmentMapper departmentMapper = Mappers.getMapper(DepartmentMapper.class);

    private DepartmentService departmentService;

    @BeforeEach
    void setUp() {
        departmentService = new DepartmentService(departmentRepositoryPort, departmentMapper);
    }

    @Test
    @DisplayName("Should create department successfully")
    void createDepartment_Success() {
        // Given
        CreateDepartmentCommand command = CreateDepartmentCommand.builder()
                .name("Finance")
                .build();

        given(departmentRepositoryPort.save(any(Department.class))).willAnswer(inv -> inv.getArgument(0));

        // When
        DepartmentResponseDto result = departmentService.createDepartment(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Finance");
        assertThat(result.getParentId()).isNull();
        verify(departmentRepositoryPort).save(any());
    }

    @Test
    @DisplayName("Should throw NotFoundException when parent department does not exist")
    void createDepartment_ParentNotFound_ThrowsNotFound() {
        // Given
        UUID parentId = UUID.randomUUID();
        CreateDepartmentCommand command = CreateDepartmentCommand.builder()
                .name("SubFinance")
                .parentId(parentId)
                .build();

        given(departmentRepositoryPort.findById(parentId)).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> departmentService.createDepartment(command))
                .isInstanceOf(NotFoundException.class);
    }
}
