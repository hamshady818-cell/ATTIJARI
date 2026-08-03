package com.awb.ged.application.port.out.persistence;

import com.awb.ged.domain.tag.model.Tag;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepositoryPort {

    Tag save(Tag tag);

    Optional<Tag> findById(UUID id);

    Optional<Tag> findByName(String name);

    List<Tag> findAll();

    void delete(UUID id);
}
