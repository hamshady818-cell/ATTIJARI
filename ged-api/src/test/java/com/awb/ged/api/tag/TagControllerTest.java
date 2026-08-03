package com.awb.ged.api.tag;

import com.awb.ged.api.exception.GlobalExceptionHandler;
import com.awb.ged.api.tag.dto.TagRequest;
import com.awb.ged.application.dto.tag.TagResponseDto;
import com.awb.ged.application.port.in.tag.*;
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

@WebMvcTest(TagController.class)
@Import({GlobalExceptionHandler.class, TagControllerTest.MethodSecurityConfig.class})
class TagControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CreateTagUseCase createTagUseCase;

    @MockitoBean
    private ListTagsUseCase listTagsUseCase;

    @MockitoBean
    private DeleteTagUseCase deleteTagUseCase;

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/tags - Success with USER role")
    void createTag_AsUser_Success() throws Exception {
        UUID tagId = UUID.randomUUID();
        TagRequest request = TagRequest.builder()
                .name("urgent")
                .description("Urgent tag")
                .build();

        TagResponseDto responseDto = TagResponseDto.builder()
                .id(tagId)
                .name("urgent")
                .description("Urgent tag")
                .build();

        given(createTagUseCase.createTag(any())).willReturn(responseDto);

        mockMvc.perform(post("/api/v1/tags")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/tags/" + tagId))
                .andExpect(jsonPath("$.id").value(tagId.toString()))
                .andExpect(jsonPath("$.name").value("urgent"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("POST /api/v1/tags - Forbidden with VIEWER role")
    void createTag_AsViewer_Forbidden() throws Exception {
        TagRequest request = TagRequest.builder()
                .name("urgent")
                .build();

        mockMvc.perform(post("/api/v1/tags")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
