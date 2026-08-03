package com.awb.ged.application.port.in.department;

import com.awb.ged.application.dto.department.DepartmentResponseDto;

import java.util.List;

public interface ListDepartmentsUseCase {
    List<DepartmentResponseDto> listDepartments();
}
