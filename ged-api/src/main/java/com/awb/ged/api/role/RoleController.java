package com.awb.ged.api.role;

import com.awb.ged.application.dto.role.RoleResponseDto;
import com.awb.ged.application.port.in.role.ListRolesUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final ListRolesUseCase listRolesUseCase;

    @Autowired
    public RoleController(ListRolesUseCase listRolesUseCase) {
        this.listRolesUseCase = listRolesUseCase;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RoleResponseDto>> listRoles() {
        List<RoleResponseDto> roles = listRolesUseCase.listRoles();
        return ResponseEntity.ok(roles);
    }
}
