package com.awb.ged.api.category;

import com.awb.ged.api.category.dto.CategoryRequest;
import com.awb.ged.api.exception.GlobalExceptionHandler;
import com.awb.ged.application.dto.category.CategoryResponseDto;
import com.awb.ged.application.port.in.category.*;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@Import({GlobalExceptionHandler.class, CategoryControllerTest.MethodSecurityConfig.class})
class CategoryControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CreateCategoryUseCase createCategoryUseCase;

    @MockitoBean
    private GetCategoryUseCase getCategoryUseCase;

    @MockitoBean
    private ListCategoriesUseCase listCategoriesUseCase;

    @MockitoBean
    private UpdateCategoryUseCase updateCategoryUseCase;

    @MockitoBean
    private DeleteCategoryUseCase deleteCategoryUseCase;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/categories - Success with ADMIN role")
    void createCategory_AsAdmin_Success() throws Exception {
        UUID categoryId = UUID.randomUUID();
        CategoryRequest request = CategoryRequest.builder()
                .name("Finance")
                .build();

        CategoryResponseDto responseDto = CategoryResponseDto.builder()
                .id(categoryId)
                .name("Finance")
                .path(categoryId.toString())
                .build();

        given(createCategoryUseCase.createCategory(any())).willReturn(responseDto);

        mockMvc.perform(post("/api/v1/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/categories/" + categoryId))
                .andExpect(jsonPath("$.id").value(categoryId.toString()))
                .andExpect(jsonPath("$.name").value("Finance"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/categories - Forbidden with USER role")
    void createCategory_AsUser_Forbidden() throws Exception {
        CategoryRequest request = CategoryRequest.builder()
                .name("Finance")
                .build();

        mockMvc.perform(post("/api/v1/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /api/v1/categories/{id} - Success with VIEWER role")
    void getCategory_AsViewer_Success() throws Exception {
        UUID categoryId = UUID.randomUUID();
        CategoryResponseDto responseDto = CategoryResponseDto.builder()
                .id(categoryId)
                .name("Finance")
                .path(categoryId.toString())
                .build();

        given(getCategoryUseCase.getCategoryById(categoryId)).willReturn(responseDto);

        mockMvc.perform(get("/api/v1/categories/{id}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoryId.toString()))
                .andExpect(jsonPath("$.name").value("Finance"));
    }
}
