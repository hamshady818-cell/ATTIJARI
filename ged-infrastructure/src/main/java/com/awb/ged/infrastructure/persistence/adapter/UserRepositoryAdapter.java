package com.awb.ged.infrastructure.persistence.adapter;

import com.awb.ged.application.port.out.persistence.UserRepositoryPort;
import com.awb.ged.domain.user.model.User;
import com.awb.ged.infrastructure.persistence.entity.department.DepartmentJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import com.awb.ged.infrastructure.persistence.repository.DepartmentJpaRepository;
import com.awb.ged.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Transactional
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository userJpaRepository;
    private final DepartmentJpaRepository departmentJpaRepository;

    public UserRepositoryAdapter(UserJpaRepository userJpaRepository,
                                 DepartmentJpaRepository departmentJpaRepository) {
        this.userJpaRepository = userJpaRepository;
        this.departmentJpaRepository = departmentJpaRepository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = mapToEntity(user);
        UserJpaEntity saved = userJpaRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByKeycloakSub(String sub) {
        return userJpaRepository.findByKeycloakId(sub).map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userJpaRepository.findAll().stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    public void delete(UUID id) {
        userJpaRepository.deleteById(id);
    }

    private User mapToDomain(UserJpaEntity entity) {
        if (entity == null) return null;
        return User.builder()
                .id(entity.getId())
                .keycloakSub(entity.getKeycloakId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .departmentId(entity.getDepartment() != null ? entity.getDepartment().getId() : null)
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private UserJpaEntity mapToEntity(User domain) {
        if (domain == null) return null;

        DepartmentJpaEntity dept = null;
        if (domain.getDepartmentId() != null) {
            dept = departmentJpaRepository.findById(domain.getDepartmentId()).orElse(null);
        }

        UserJpaEntity entity = UserJpaEntity.builder()
                .keycloakId(domain.getKeycloakSub())
                .username(domain.getUsername())
                .email(domain.getEmail())
                .firstName(domain.getFirstName())
                .lastName(domain.getLastName())
                .department(dept)
                .active(domain.isActive())
                .build();

        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        return entity;
    }
}
