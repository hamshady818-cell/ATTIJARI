package com.awb.ged.infrastructure.persistence.repository;

import com.awb.ged.infrastructure.persistence.entity.department.DepartmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentJpaRepository extends JpaRepository<DepartmentJpaEntity, UUID> {

    Optional<DepartmentJpaEntity> findByName(String name);
}
