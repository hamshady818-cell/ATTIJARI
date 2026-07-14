package com.awb.ged.common.exception;

/**
 * <h1>ErrorCategory</h1>
 * <p>
 * Represents the functional or technical category of an error in the GED-AWB system.
 * This is used for classification, logs filtering, and to help client applications
 * understand the source/context of the error.
 * </p>
 */
public enum ErrorCategory {
    /** Errors related to document operations (metadata, upload, update, delete, etc.) */
    DOCUMENT,

    /** Errors related to folder structure and operations */
    FOLDER,

    /** Errors related to user profiles, synchronization or retrieval */
    USER,

    /** Technical errors from physical document storage (filesystem, MinIO, S3) */
    STORAGE,

    /** Errors arising from AI pipeline, OCR, LLM, or embeddings processes */
    AI,

    /** Authentication, authorization, token validation, and RBAC errors */
    SECURITY,

    /** Unexpected system errors, resource exhaustion, or technical database failures */
    SYSTEM
}
