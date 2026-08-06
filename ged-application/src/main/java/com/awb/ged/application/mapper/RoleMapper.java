package com.awb.ged.application.mapper;

import com.awb.ged.application.dto.role.RoleResponseDto;
import com.awb.ged.domain.role.model.Role;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    RoleResponseDto toResponseDto(Role role);
}
