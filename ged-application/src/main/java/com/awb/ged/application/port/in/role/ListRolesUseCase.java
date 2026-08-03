package com.awb.ged.application.port.in.role;

import com.awb.ged.application.dto.role.RoleResponseDto;

import java.util.List;

public interface ListRolesUseCase {
    List<RoleResponseDto> listRoles();
}
