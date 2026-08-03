package com.awb.ged.api.metadata;

import com.awb.ged.api.exception.GlobalExceptionHandler;
import com.awb.ged.api.metadata.dto.MetadataDefinitionRequest;
import com.awb.ged.application.dto.metadata.MetadataDefinitionResponseDto;
import com.awb.ged.application.port.in.metadata.*;
import com.awb.ged.domain.metadata.model.MetadataType;
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

@WebMvcTest(MetadataDefinitionController.class)
@Import({GlobalExceptionHandler.class, MetadataDefinitionControllerTest.MethodSecurityConfig.class})
class MetadataDefinitionControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CreateMetadataDefinitionUseCase createUseCase;

    @MockitoBean
    private GetMetadataDefinitionUseCase getUseCase;

    @MockitoBean
    private ListMetadataDefinitionsUseCase listUseCase;

    @MockitoBean
    private UpdateMetadataDefinitionUseCase updateUseCase;

    @MockitoBean
    private DeleteMetadataDefinitionUseCase deleteUseCase;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/metadata-definitions - Success with ADMIN role")
    void createMetadataDefinition_AsAdmin_Success() throws Exception {
        UUID defId = UUID.randomUUID();
        MetadataDefinitionRequest request = MetadataDefinitionRequest.builder()
                .name("invoice_number")
                .label("Invoice Number")
                .type(MetadataType.STRING)
                .required(true)
                .build();

        MetadataDefinitionResponseDto responseDto = MetadataDefinitionResponseDto.builder()
                .id(defId)
                .name("invoice_number")
                .label("Invoice Number")
                .type(MetadataType.STRING)
                .required(true)
                .build();

        given(createUseCase.createMetadataDefinition(any())).willReturn(responseDto);

        mockMvc.perform(post("/api/v1/metadata-definitions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/metadata-definitions/" + defId))
                .andExpect(jsonPath("$.id").value(defId.toString()))
                .andExpect(jsonPath("$.name").value("invoice_number"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/metadata-definitions - Forbidden with USER role")
    void createMetadataDefinition_AsUser_Forbidden() throws Exception {
        MetadataDefinitionRequest request = MetadataDefinitionRequest.builder()
                .name("invoice_number")
                .label("Invoice Number")
                .type(MetadataType.STRING)
                .required(true)
                .build();

        mockMvc.perform(post("/api/v1/metadata-definitions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
