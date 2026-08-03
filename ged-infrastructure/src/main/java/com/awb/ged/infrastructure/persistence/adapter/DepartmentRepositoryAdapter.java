package com.awb.ged.infrastructure.persistence.adapter;

import com.awb.ged.application.port.out.persistence.DepartmentRepositoryPort;
import com.awb.ged.domain.department.model.Department;
import com.awb.ged.infrastructure.persistence.entity.department.DepartmentJpaEntity;
import com.awb.ged.infrastructure.persistence.repository.DepartmentJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Transactional
public class DepartmentRepositoryAdapter implements DepartmentRepositoryPort {

    private final DepartmentJpaRepository departmentJpaRepository;

    public DepartmentRepositoryAdapter(DepartmentJpaRepository departmentJpaRepository) {
        this.departmentJpaRepository = departmentJpaRepository;
    }

    @Override
    public Department save(Department department) {
        DepartmentJpaEntity entity = mapToEntity(department);
        DepartmentJpaEntity saved = departmentJpaRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Department> findById(UUID id) {
        return departmentJpaRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Department> findAll() {
        return departmentJpaRepository.findAll().stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    public void delete(UUID id) {
        departmentJpaRepository.deleteById(id);
    }

    private Department mapToDomain(DepartmentJpaEntity entity) {
        if (entity == null) return null;
        return Department.builder()
                .id(entity.getId())
                .name(entity.getName())
                .parentId(entity.getParentDepartment() != null ? entity.getParentDepartment().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private DepartmentJpaEntity mapToEntity(Department domain) {
        if (domain == null) return null;
        DepartmentJpaEntity parent = null;
        if (domain.getParentId() != null) {
            parent = departmentJpaRepository.findById(domain.getParentId()).orElse(null);
        }
        DepartmentJpaEntity entity = DepartmentJpaEntity.builder()
                .name(domain.getName())
                .parentDepartment(parent)
                .build();
        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        return entity;
    }
}
