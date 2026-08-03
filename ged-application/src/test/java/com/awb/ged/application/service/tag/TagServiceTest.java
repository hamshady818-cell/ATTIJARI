package com.awb.ged.application.service.tag;

import com.awb.ged.application.dto.tag.CreateTagCommand;
import com.awb.ged.application.dto.tag.TagResponseDto;
import com.awb.ged.application.mapper.TagMapper;
import com.awb.ged.application.port.out.persistence.TagRepositoryPort;
import com.awb.ged.common.exception.ConflictException;
import com.awb.ged.common.exception.InvalidRequestException;
import com.awb.ged.domain.tag.model.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepositoryPort tagRepositoryPort;

    private final TagMapper tagMapper = Mappers.getMapper(TagMapper.class);

    private TagService tagService;

    @BeforeEach
    void setUp() {
        tagService = new TagService(tagRepositoryPort, tagMapper);
    }

    @Test
    @DisplayName("Should create normalized tag successfully")
    void createTag_Success() {
        // Given
        CreateTagCommand command = CreateTagCommand.builder()
                .name("Urgent Info")
                .description("Urgent elements tag")
                .build();

        given(tagRepositoryPort.findByName("urgent-info")).willReturn(Optional.empty());
        given(tagRepositoryPort.save(any(Tag.class))).willAnswer(inv -> inv.getArgument(0));

        // When
        TagResponseDto result = tagService.createTag(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("urgent-info");
        assertThat(result.getDescription()).isEqualTo("Urgent elements tag");
        verify(tagRepositoryPort).save(any());
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when tag name has invalid format")
    void createTag_InvalidNameFormat_ThrowsInvalidRequest() {
        // Given
        CreateTagCommand command = CreateTagCommand.builder()
                .name("!!!")
                .build();

        // When / Then
        assertThatThrownBy(() -> tagService.createTag(command))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("Should throw ConflictException when tag name already exists")
    void createTag_DuplicateName_ThrowsConflict() {
        // Given
        CreateTagCommand command = CreateTagCommand.builder()
                .name("urgent")
                .build();

        given(tagRepositoryPort.findByName("urgent")).willReturn(Optional.of(Tag.builder().build()));

        // When / Then
        assertThatThrownBy(() -> tagService.createTag(command))
                .isInstanceOf(ConflictException.class);
    }
}
