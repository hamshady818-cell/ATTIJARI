package com.awb.ged.application.service.department;

import com.awb.ged.application.dto.department.CreateDepartmentCommand;
import com.awb.ged.application.dto.department.DepartmentResponseDto;
import com.awb.ged.application.dto.department.UpdateDepartmentCommand;
import com.awb.ged.application.mapper.DepartmentMapper;
import com.awb.ged.application.port.in.department.*;
import com.awb.ged.application.port.out.persistence.DepartmentRepositoryPort;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.department.model.Department;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DepartmentService implements CreateDepartmentUseCase, GetDepartmentUseCase, ListDepartmentsUseCase, UpdateDepartmentUseCase, DeleteDepartmentUseCase {

    private final DepartmentRepositoryPort departmentRepositoryPort;
    private final DepartmentMapper departmentMapper;

    @Autowired
    public DepartmentService(DepartmentRepositoryPort departmentRepositoryPort, DepartmentMapper departmentMapper) {
        this.departmentRepositoryPort = departmentRepositoryPort;
        this.departmentMapper = departmentMapper;
    }

    @Override
    public DepartmentResponseDto createDepartment(CreateDepartmentCommand command) {
        if (command.getParentId() != null) {
            departmentRepositoryPort.findById(command.getParentId())
                    .orElseThrow(() -> new NotFoundException(
                            ErrorCode.DEPARTMENT_NOT_FOUND,
                            "Parent department with ID " + command.getParentId() + " was not found."
                    ));
        }

        Department department = Department.builder()
                .id(UUID.randomUUID())
                .name(command.getName())
                .parentId(command.getParentId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Department saved = departmentRepositoryPort.save(department);
        return departmentMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponseDto getDepartmentById(UUID id) {
        Department department = departmentRepositoryPort.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DEPARTMENT_NOT_FOUND,
                        "Department with ID " + id + " was not found."
                ));
        return departmentMapper.toResponseDto(department);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponseDto> listDepartments() {
        List<Department> list = departmentRepositoryPort.findAll();
        return list.stream()
                .map(departmentMapper::toResponseDto)
                .toList();
    }

    @Override
    public DepartmentResponseDto updateDepartment(UpdateDepartmentCommand command) {
        Department department = departmentRepositoryPort.findById(command.getId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DEPARTMENT_NOT_FOUND,
                        "Department with ID " + command.getId() + " was not found."
                ));

        if (command.getParentId() != null && !command.getParentId().equals(department.getParentId())) {
            departmentRepositoryPort.findById(command.getParentId())
                    .orElseThrow(() -> new NotFoundException(
                            ErrorCode.DEPARTMENT_NOT_FOUND,
                            "Parent department with ID " + command.getParentId() + " was not found."
                    ));
        }

        Department updated = department.toBuilder()
                .name(command.getName())
                .parentId(command.getParentId())
                .updatedAt(Instant.now())
                .build();

        Department saved = departmentRepositoryPort.save(updated);
        return departmentMapper.toResponseDto(saved);
    }

    @Override
    public void deleteDepartment(UUID id) {
        departmentRepositoryPort.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DEPARTMENT_NOT_FOUND,
                        "Department with ID " + id + " was not found."
                ));
        departmentRepositoryPort.delete(id);
    }
}
