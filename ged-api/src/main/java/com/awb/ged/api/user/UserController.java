package com.awb.ged.api.user;

import com.awb.ged.application.dto.user.UserResponseDto;
import com.awb.ged.application.port.out.persistence.UserRepositoryPort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepositoryPort userRepositoryPort;

    public UserController(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER') or hasRole('VIEWER')")
    public ResponseEntity<List<UserResponseDto>> listUsers() {
        List<UserResponseDto> users = userRepositoryPort.findAll().stream()
                .map(u -> UserResponseDto.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .firstName(u.getFirstName())
                        .lastName(u.getLastName())
                        .email(u.getEmail())
                        .departmentId(u.getDepartmentId())
                        .active(u.isActive())
                        .build())
                .toList();
        return ResponseEntity.ok(users);
    }
}
