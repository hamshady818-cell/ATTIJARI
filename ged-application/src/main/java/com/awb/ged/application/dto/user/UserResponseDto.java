package com.awb.ged.application.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

    private UUID id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private UUID departmentId;
    private boolean active;
}
