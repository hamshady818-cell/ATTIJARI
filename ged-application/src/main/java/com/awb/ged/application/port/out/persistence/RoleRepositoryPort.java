package com.awb.ged.application.port.out.persistence;

import com.awb.ged.domain.role.model.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepositoryPort {

    Optional<Role> findById(UUID id);

    List<Role> findAll();
}
