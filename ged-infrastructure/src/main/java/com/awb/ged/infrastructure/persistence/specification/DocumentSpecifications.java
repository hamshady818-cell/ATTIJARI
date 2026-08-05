package com.awb.ged.infrastructure.persistence.specification;

import com.awb.ged.application.dto.document.DocumentSearchQuery;
import com.awb.ged.infrastructure.persistence.entity.document.DocumentJpaEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications for dynamic document search.
 * Builds a composed Predicate from the fields present in {@link DocumentSearchQuery}.
 */
public final class DocumentSpecifications {

    private DocumentSpecifications() {}

    public static Specification<DocumentJpaEntity> buildSearch(DocumentSearchQuery query) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always exclude soft-deleted documents
            predicates.add(cb.isNull(root.get("deletedAt")));

            // Keyword search: match against title or description (partial, case-insensitive)
            if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
                String pattern = "%" + query.getKeyword().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(titleMatch, descMatch));
            }

            // Status filter
            if (query.getStatus() != null && !query.getStatus().isBlank()) {
                try {
                    DocumentJpaEntity.DocumentStatus status =
                            DocumentJpaEntity.DocumentStatus.valueOf(query.getStatus().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), status));
                } catch (IllegalArgumentException ignored) {
                    // Invalid status — ignore filter
                }
            }

            // Owner filter
            if (query.getOwnerId() != null) {
                predicates.add(cb.equal(root.get("owner").get("id"), query.getOwnerId()));
            }

            // Folder filter
            if (query.getFolderId() != null) {
                predicates.add(cb.equal(root.get("folder").get("id"), query.getFolderId()));
            }

            // Tag filter (join document_tags table)
            if (query.getTagName() != null && !query.getTagName().isBlank()) {
                String tagPattern = "%" + query.getTagName().toLowerCase() + "%";
                var tagJoin = root.join("tags");
                predicates.add(cb.like(cb.lower(tagJoin.get("name")), tagPattern));
                criteriaQuery.distinct(true);
            }

            // Category filter (join document_categories table)
            if (query.getCategoryId() != null) {
                var categoryJoin = root.join("categories");
                predicates.add(cb.equal(categoryJoin.get("id"), query.getCategoryId()));
                criteriaQuery.distinct(true);
            }

            // Date range filters
            if (query.getCreatedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), query.getCreatedFrom()));
            }
            if (query.getCreatedTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), query.getCreatedTo()));
            }
            if (query.getUpdatedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("updatedAt"), query.getUpdatedFrom()));
            }
            if (query.getUpdatedTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("updatedAt"), query.getUpdatedTo()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
