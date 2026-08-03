package com.awb.ged.application.port.in.department;

import com.awb.ged.application.dto.department.CreateDepartmentCommand;
import com.awb.ged.application.dto.department.DepartmentResponseDto;

public interface CreateDepartmentUseCase {
    DepartmentResponseDto createDepartment(CreateDepartmentCommand command);
}
