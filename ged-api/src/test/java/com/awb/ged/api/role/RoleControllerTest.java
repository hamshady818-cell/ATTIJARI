package com.awb.ged.api.role;

import com.awb.ged.api.exception.GlobalExceptionHandler;
import com.awb.ged.application.dto.role.RoleResponseDto;
import com.awb.ged.application.port.in.role.ListRolesUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoleController.class)
@Import({GlobalExceptionHandler.class, RoleControllerTest.MethodSecurityConfig.class})
class RoleControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListRolesUseCase listRolesUseCase;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/roles - Success with ADMIN role")
    void listRoles_AsAdmin_Success() throws Exception {
        UUID roleId = UUID.randomUUID();
        RoleResponseDto responseDto = RoleResponseDto.builder()
                .id(roleId)
                .name("ADMIN")
                .description("Administrator")
                .build();

        given(listRolesUseCase.listRoles()).willReturn(List.of(responseDto));

        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(roleId.toString()))
                .andExpect(jsonPath("$[0].name").value("ADMIN"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/roles - Forbidden with USER role")
    void listRoles_AsUser_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isForbidden());
    }
}
