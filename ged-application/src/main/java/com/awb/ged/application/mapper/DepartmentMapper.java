package com.awb.ged.application.mapper;

import com.awb.ged.application.dto.department.DepartmentResponseDto;
import com.awb.ged.domain.department.model.Department;
import org.mapstruct.Mapper;

@Mapper
public interface DepartmentMapper {
    DepartmentResponseDto toResponseDto(Department department);
}
