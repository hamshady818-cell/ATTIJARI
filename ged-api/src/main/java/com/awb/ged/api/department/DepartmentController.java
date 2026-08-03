package com.awb.ged.api.department;

import com.awb.ged.api.department.dto.DepartmentRequest;
import com.awb.ged.application.dto.department.DepartmentResponseDto;
import com.awb.ged.application.dto.department.CreateDepartmentCommand;
import com.awb.ged.application.dto.department.UpdateDepartmentCommand;
import com.awb.ged.application.port.in.department.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {

    private final CreateDepartmentUseCase createDepartmentUseCase;
    private final GetDepartmentUseCase getDepartmentUseCase;
    private final ListDepartmentsUseCase listDepartmentsUseCase;
    private final UpdateDepartmentUseCase updateDepartmentUseCase;
    private final DeleteDepartmentUseCase deleteDepartmentUseCase;

    @Autowired
    public DepartmentController(CreateDepartmentUseCase createDepartmentUseCase,
                                GetDepartmentUseCase getDepartmentUseCase,
                                ListDepartmentsUseCase listDepartmentsUseCase,
                                UpdateDepartmentUseCase updateDepartmentUseCase,
                                DeleteDepartmentUseCase deleteDepartmentUseCase) {
        this.createDepartmentUseCase = createDepartmentUseCase;
        this.getDepartmentUseCase = getDepartmentUseCase;
        this.listDepartmentsUseCase = listDepartmentsUseCase;
        this.updateDepartmentUseCase = updateDepartmentUseCase;
        this.deleteDepartmentUseCase = deleteDepartmentUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<DepartmentResponseDto> createDepartment(@Valid @RequestBody DepartmentRequest request) {
        CreateDepartmentCommand command = CreateDepartmentCommand.builder()
                .name(request.getName())
                .parentId(request.getParentId())
                .build();
        DepartmentResponseDto created = createDepartmentUseCase.createDepartment(command);
        URI location = URI.create("/api/v1/departments/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER') or hasRole('VIEWER')")
    public ResponseEntity<DepartmentResponseDto> getDepartmentById(@PathVariable("id") UUID id) {
        DepartmentResponseDto department = getDepartmentUseCase.getDepartmentById(id);
        return ResponseEntity.ok(department);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER') or hasRole('VIEWER')")
    public ResponseEntity<List<DepartmentResponseDto>> listDepartments() {
        List<DepartmentResponseDto> list = listDepartmentsUseCase.listDepartments();
        return ResponseEntity.ok(list);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<DepartmentResponseDto> updateDepartment(
            @PathVariable("id") UUID id,
            @Valid @RequestBody DepartmentRequest request) {
        UpdateDepartmentCommand command = UpdateDepartmentCommand.builder()
                .id(id)
                .name(request.getName())
                .parentId(request.getParentId())
                .build();
        DepartmentResponseDto updated = updateDepartmentUseCase.updateDepartment(command);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Void> deleteDepartment(@PathVariable("id") UUID id) {
        deleteDepartmentUseCase.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }
}
