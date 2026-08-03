package com.awb.ged.application.port.in.department;

import com.awb.ged.application.dto.department.DepartmentResponseDto;

import java.util.UUID;

public interface GetDepartmentUseCase {
    DepartmentResponseDto getDepartmentById(UUID id);
}
