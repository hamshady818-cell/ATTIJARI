package com.awb.ged.infrastructure.persistence.entity.ai;

import com.awb.ged.infrastructure.persistence.entity.shared.BaseEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * <h1>AiChatSessionJpaEntity</h1>
 * <p>
 * JPA entity representing a RAG (Retrieval-Augmented Generation) conversation session
 * between a user and the GED-AWB AI assistant.
 * </p>
 *
 * <p>
 * Each session has a <strong>retrieval scope</strong> that controls which documents
 * the AI can retrieve context from when answering user questions:
 * <ul>
 *   <li>{@code DOCUMENT} — answers grounded on a single document.</li>
 *   <li>{@code FOLDER}   — answers grounded on all documents in a folder.</li>
 *   <li>{@code CATEGORY} — answers grounded on all documents in a category tree.</li>
 *   <li>{@code GLOBAL}   — answers grounded on all accessible documents in the corpus.</li>
 * </ul>
 * </p>
 *
 * <p>
 * The {@code scopeEntityId} stores the UUID of the target entity (document, folder,
 * or category) and is {@code null} for {@code GLOBAL} sessions.
 * </p>
 *
 * <p><strong>Relationships:</strong></p>
 * <ul>
 *   <li>M:1 → {@link UserJpaEntity} — the session owner.</li>
 *   <li>1:N → {@link AiChatMessageJpaEntity} — the messages within this session.</li>
 * </ul>
 *
 * <p><strong>Design Decision — Polymorphic scope reference:</strong>
 * The scope target (document / folder / category) is stored as a UUID + enum pair
 * rather than typed FK columns, because the scope type may evolve (e.g., adding
 * tag-scoped sessions in the future) without schema migrations.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "ai_chat_sessions",
        indexes = {
                @Index(name = "idx_chat_sessions_user_id",          columnList = "user_id"),
                @Index(name = "idx_chat_sessions_last_activity_at", columnList = "user_id, last_activity_at DESC")
        }
)
public class AiChatSessionJpaEntity extends BaseEntity {

    /**
     * Controls which documents are in scope for RAG retrieval during this session.
     */
    public enum ScopeType {
        /** Grounded on a single specific document */
        DOCUMENT,
        /** Grounded on all documents within a specific folder (recursively) */
        FOLDER,
        /** Grounded on all documents within a specific category tree */
        CATEGORY,
        /** Grounded on all documents accessible to the user */
        GLOBAL
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Ownership
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The user who started this conversation session.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_chat_sessions_user")
    )
    private UserJpaEntity user;

    // ─────────────────────────────────────────────────────────────────────────
    //  Session identity
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Human-readable title of the session.
     * Auto-generated from the first user message if not provided explicitly.
     * Example: {@code "Analyse du contrat fournisseur XYZ"}.
     */
    @Size(max = 255)
    @Column(name = "title", length = 255)
    private String title;

    // ─────────────────────────────────────────────────────────────────────────
    //  Retrieval scope
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Determines which part of the document corpus is searched for context chunks.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 30)
    private ScopeType scopeType;

    /**
     * UUID of the scoping entity (document, folder, or category).
     * {@code null} when {@code scopeType = GLOBAL}.
     */
    @Column(name = "scope_entity_id")
    private UUID scopeEntityId;

    // ─────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Whether this session is still active.
     * Inactive sessions are hidden from the UI but retained for history.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Timestamp of the most recent message in this session.
     * Updated on every new message — used for "recent sessions" ordering.
     */
    @Column(name = "last_activity_at", nullable = false)
    @Builder.Default
    private Instant lastActivityAt = Instant.now();

    // ─────────────────────────────────────────────────────────────────────────
    //  Messages
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Ordered list of all messages in this session (user questions + AI answers).
     * Ordered chronologically by {@code createdAt ASC}.
     */
    @OneToMany(
            mappedBy      = "session",
            fetch         = FetchType.LAZY,
            cascade       = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<AiChatMessageJpaEntity> messages = new ArrayList<>();
}
