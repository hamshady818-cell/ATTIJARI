package com.awb.ged.application.service.role;

import com.awb.ged.application.dto.role.RoleResponseDto;
import com.awb.ged.application.mapper.RoleMapper;
import com.awb.ged.application.port.in.role.ListRolesUseCase;
import com.awb.ged.application.port.out.persistence.RoleRepositoryPort;
import com.awb.ged.domain.role.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RoleService implements ListRolesUseCase {

    private final RoleRepositoryPort roleRepositoryPort;
    private final RoleMapper roleMapper;

    @Autowired
    public RoleService(RoleRepositoryPort roleRepositoryPort, RoleMapper roleMapper) {
        this.roleRepositoryPort = roleRepositoryPort;
        this.roleMapper = roleMapper;
    }

    @Override
    public List<RoleResponseDto> listRoles() {
        List<Role> list = roleRepositoryPort.findAll();
        return list.stream()
                .map(roleMapper::toResponseDto)
                .toList();
    }
}
