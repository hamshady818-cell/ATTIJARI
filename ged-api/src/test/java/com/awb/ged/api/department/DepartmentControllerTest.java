package com.awb.ged.api.department;

import com.awb.ged.api.department.dto.DepartmentRequest;
import com.awb.ged.api.exception.GlobalExceptionHandler;
import com.awb.ged.application.dto.department.DepartmentResponseDto;
import com.awb.ged.application.port.in.department.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DepartmentController.class)
@Import({GlobalExceptionHandler.class, DepartmentControllerTest.MethodSecurityConfig.class})
class DepartmentControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CreateDepartmentUseCase createDepartmentUseCase;

    @MockitoBean
    private GetDepartmentUseCase getDepartmentUseCase;

    @MockitoBean
    private ListDepartmentsUseCase listDepartmentsUseCase;

    @MockitoBean
    private UpdateDepartmentUseCase updateDepartmentUseCase;

    @MockitoBean
    private DeleteDepartmentUseCase deleteDepartmentUseCase;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/departments - Success with ADMIN role")
    void createDepartment_AsAdmin_Success() throws Exception {
        UUID deptId = UUID.randomUUID();
        DepartmentRequest request = DepartmentRequest.builder()
                .name("HR")
                .build();

        DepartmentResponseDto responseDto = DepartmentResponseDto.builder()
                .id(deptId)
                .name("HR")
                .build();

        given(createDepartmentUseCase.createDepartment(any())).willReturn(responseDto);

        mockMvc.perform(post("/api/v1/departments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/departments/" + deptId))
                .andExpect(jsonPath("$.id").value(deptId.toString()))
                .andExpect(jsonPath("$.name").value("HR"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/departments - Forbidden with USER role")
    void createDepartment_AsUser_Forbidden() throws Exception {
        DepartmentRequest request = DepartmentRequest.builder()
                .name("HR")
                .build();

        mockMvc.perform(post("/api/v1/departments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /api/v1/departments/{id} - Success with VIEWER role")
    void getDepartment_AsViewer_Success() throws Exception {
        UUID deptId = UUID.randomUUID();
        DepartmentResponseDto responseDto = DepartmentResponseDto.builder()
                .id(deptId)
                .name("HR")
                .build();

        given(getDepartmentUseCase.getDepartmentById(deptId)).willReturn(responseDto);

        mockMvc.perform(get("/api/v1/departments/{id}", deptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(deptId.toString()))
                .andExpect(jsonPath("$.name").value("HR"));
    }
}
