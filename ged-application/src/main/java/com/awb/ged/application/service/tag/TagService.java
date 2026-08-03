package com.awb.ged.application.service.tag;

import com.awb.ged.application.dto.tag.CreateTagCommand;
import com.awb.ged.application.dto.tag.TagResponseDto;
import com.awb.ged.application.mapper.TagMapper;
import com.awb.ged.application.port.in.tag.*;
import com.awb.ged.application.port.out.persistence.TagRepositoryPort;
import com.awb.ged.common.exception.ConflictException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.InvalidRequestException;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.tag.model.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TagService implements CreateTagUseCase, ListTagsUseCase, DeleteTagUseCase {

    private final TagRepositoryPort tagRepositoryPort;
    private final TagMapper tagMapper;

    @Autowired
    public TagService(TagRepositoryPort tagRepositoryPort, TagMapper tagMapper) {
        this.tagRepositoryPort = tagRepositoryPort;
        this.tagMapper = tagMapper;
    }

    @Override
    public TagResponseDto createTag(CreateTagCommand command) {
        String originalName = command.getName();
        if (originalName == null || originalName.trim().isEmpty()) {
            throw new InvalidRequestException(ErrorCode.INVALID_INPUT, "Tag name cannot be empty.");
        }

        // Normalize name into a lowercase slug
        String slug = originalName.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (slug.isEmpty() || !slug.matches("^[a-z0-9]+(?:-[a-z0-9]+)*$")) {
            throw new InvalidRequestException(
                    ErrorCode.INVALID_INPUT,
                    "Tag name must represent a valid alphanumeric slug (e.g., 'my-tag-name'). Got: " + originalName
            );
        }

        // Uniqueness validation
        Optional<Tag> existing = tagRepositoryPort.findByName(slug);
        if (existing.isPresent()) {
            throw new ConflictException(
                    ErrorCode.INVALID_INPUT,
                    "A tag with the slug name '" + slug + "' already exists."
            );
        }

        Tag tag = Tag.builder()
                .id(UUID.randomUUID())
                .name(slug)
                .description(command.getDescription())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Tag saved = tagRepositoryPort.save(tag);
        return tagMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagResponseDto> listTags() {
        List<Tag> list = tagRepositoryPort.findAll();
        return list.stream()
                .map(tagMapper::toResponseDto)
                .toList();
    }

    @Override
    public void deleteTag(UUID id) {
        tagRepositoryPort.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.INVALID_INPUT,
                        "Tag with ID " + id + " was not found."
                ));
        tagRepositoryPort.delete(id);
    }
}
