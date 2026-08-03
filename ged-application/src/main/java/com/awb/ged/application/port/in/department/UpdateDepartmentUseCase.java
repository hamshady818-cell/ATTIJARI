package com.awb.ged.application.port.in.department;

import com.awb.ged.application.dto.department.DepartmentResponseDto;
import com.awb.ged.application.dto.department.UpdateDepartmentCommand;

public interface UpdateDepartmentUseCase {
    DepartmentResponseDto updateDepartment(UpdateDepartmentCommand command);
}
