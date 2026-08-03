package com.awb.ged.api.tag;

import com.awb.ged.api.tag.dto.TagRequest;
import com.awb.ged.application.dto.tag.TagResponseDto;
import com.awb.ged.application.dto.tag.CreateTagCommand;
import com.awb.ged.application.port.in.tag.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

    private final CreateTagUseCase createTagUseCase;
    private final ListTagsUseCase listTagsUseCase;
    private final DeleteTagUseCase deleteTagUseCase;

    @Autowired
    public TagController(CreateTagUseCase createTagUseCase,
                         ListTagsUseCase listTagsUseCase,
                         DeleteTagUseCase deleteTagUseCase) {
        this.createTagUseCase = createTagUseCase;
        this.listTagsUseCase = listTagsUseCase;
        this.deleteTagUseCase = deleteTagUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<TagResponseDto> createTag(@Valid @RequestBody TagRequest request) {
        CreateTagCommand command = CreateTagCommand.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        TagResponseDto created = createTagUseCase.createTag(command);
        URI location = URI.create("/api/v1/tags/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER') or hasRole('VIEWER')")
    public ResponseEntity<List<TagResponseDto>> listTags() {
        List<TagResponseDto> list = listTagsUseCase.listTags();
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER')")
    public ResponseEntity<Void> deleteTag(@PathVariable("id") UUID id) {
        deleteTagUseCase.deleteTag(id);
        return ResponseEntity.noContent().build();
    }
}
