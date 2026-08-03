package com.awb.ged.application.dto.department;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResponseDto {
    private UUID id;
    private String name;
    private UUID parentId;
    private Instant createdAt;
    private Instant updatedAt;
}
