package com.awb.ged.common.security;

/**
 * <h1>AppPermission</h1>
 * <p>
 * Defines fine-grained application permissions organized by functional domains.
 * These permissions are checked dynamically in the backend (using a custom resolver/filter)
 * to restrict or allow specific actions on entities.
 * </p>
 */
public enum AppPermission {

    // --- DOCUMENT DOMAIN ---
    /** View documents and download files */
    DOCUMENT_READ,
    /** Upload new documents and check-in new versions */
    DOCUMENT_CREATE,
    /** Modify metadata, lock/unlock (check-out) documents */
    DOCUMENT_UPDATE,
    /** Delete or move documents to trash */
    DOCUMENT_DELETE,

    // --- FOLDER DOMAIN ---
    /** View folder structures and list contents */
    FOLDER_READ,
    /** Create new folders */
    FOLDER_CREATE,
    /** Rename, move, or modify folder permissions */
    FOLDER_UPDATE,
    /** Delete folders */
    FOLDER_DELETE,

    // --- USER & IDENTITY MANAGEMENT ---
    /** Create, modify, and delete user profiles and synchronization settings */
    USER_MANAGE,
    /** Manage roles assignation and permissions mapping */
    ROLE_MANAGE,

    // --- AUDIT SYSTEM ---
    /** Read immutable audit logs and timelines */
    AUDIT_READ,

    // --- ARTIFICIAL INTELLIGENCE ---
    /** Interactively converse with the RAG assistant */
    AI_CHAT,
    /** Request automated LLM summaries of documents */
    AI_SUMMARIZE,
    /** Perform semantic vector search on documents content */
    AI_SEARCH
}
