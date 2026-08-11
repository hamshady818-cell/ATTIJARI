package com.awb.ged.common.exception;

/**
 * <h1>ErrorCode</h1>
 * <p>
 * Standardized error codes for the GED-AWB application.
 * Each error code provides a stable, unique identifier, a default message,
 * and a specific {@link ErrorCategory}.
 * These codes are designed to be easily processed by client applications and used for i18n mapping.
 * </p>
 */
public enum ErrorCode {
    
    // --- SYSTEM & REQUEST GENERAL ERRORS (ERR-SYS-xxx) ---
    INTERNAL_ERROR("ERR-SYS-001", "An unexpected internal error occurred.", ErrorCategory.SYSTEM),
    INVALID_INPUT("ERR-SYS-002", "The request contains invalid or malformed data.", ErrorCategory.SYSTEM),
    RESOURCE_LOCKED("ERR-SYS-003", "The requested resource is currently locked by another operation.", ErrorCategory.SYSTEM),
    
    // --- SECURITY & AUTHORIZATION (ERR-SEC-xxx) ---
    UNAUTHORIZED("ERR-SEC-001", "Authentication is required to access this resource.", ErrorCategory.SECURITY),
    FORBIDDEN("ERR-SEC-002", "You do not have the required permissions to perform this action.", ErrorCategory.SECURITY),
    TOKEN_EXPIRED("ERR-SEC-003", "The provided authentication token has expired.", ErrorCategory.SECURITY),
    TOKEN_INVALID("ERR-SEC-004", "The provided authentication token is invalid or corrupt.", ErrorCategory.SECURITY),
    
    // --- DOCUMENT MANAGEMENT (ERR-DOC-xxx) ---
    DOCUMENT_NOT_FOUND("ERR-DOC-001", "The requested document was not found.", ErrorCategory.DOCUMENT),
    DOCUMENT_DUPLICATE("ERR-DOC-002", "A document with the same hash (duplicate content) already exists.", ErrorCategory.DOCUMENT),
    DOCUMENT_LOCKED("ERR-DOC-003", "The document is locked by another user (check-out active).", ErrorCategory.DOCUMENT),
    DOCUMENT_ALREADY_EXISTS("ERR-DOC-004", "A document with the same name already exists in this folder.", ErrorCategory.DOCUMENT),
    INVALID_DOCUMENT_FORMAT("ERR-DOC-005", "The document format is not supported.", ErrorCategory.DOCUMENT),
    FILE_TOO_LARGE("ERR-DOC-006", "The uploaded file exceeds the maximum allowed size.", ErrorCategory.DOCUMENT),
    CATEGORY_NOT_FOUND("ERR-DOC-007", "The specified category was not found.", ErrorCategory.DOCUMENT),
    
    // --- FOLDER MANAGEMENT (ERR-FLD-xxx) ---
    FOLDER_NOT_FOUND("ERR-FLD-001", "The requested folder was not found.", ErrorCategory.FOLDER),
    FOLDER_DUPLICATE("ERR-FLD-002", "A folder with the same name already exists in the target location.", ErrorCategory.FOLDER),
    FOLDER_CYCLE_DETECTED("ERR-FLD-003", "Cannot move folder: would create a circular dependency.", ErrorCategory.FOLDER),
    FOLDER_NOT_EMPTY("ERR-FLD-004", "The folder is not empty and cannot be deleted.", ErrorCategory.FOLDER),
    
    // --- USER MANAGEMENT (ERR-USR-xxx) ---
    USER_NOT_FOUND("ERR-USR-001", "The specified user was not found.", ErrorCategory.USER),
    ROLE_NOT_FOUND("ERR-USR-002", "The specified role was not found.", ErrorCategory.USER),
    DEPARTMENT_NOT_FOUND("ERR-USR-003", "The specified department was not found.", ErrorCategory.USER),

    // --- STORAGE ENGINES (ERR-STG-xxx) ---
    STORAGE_WRITE_ERROR("ERR-STG-001", "Failed to write document content to the physical storage.", ErrorCategory.STORAGE),
    STORAGE_READ_ERROR("ERR-STG-002", "Failed to read document content from the physical storage.", ErrorCategory.STORAGE),
    STORAGE_DELETE_ERROR("ERR-STG-003", "Failed to delete document from the physical storage.", ErrorCategory.STORAGE),
    
    // --- AI PIPELINE & PROCESSING (ERR-AI-xxx) ---
    OCR_PROCESSING_FAILED("ERR-AI-001", "Failed to perform OCR text extraction on the document.", ErrorCategory.AI),
    LLM_GENERATE_FAILED("ERR-AI-002", "Failed to generate metadata or summary using the LLM model.", ErrorCategory.AI),
    EMBEDDING_GENERATE_FAILED("ERR-AI-003", "Failed to generate vector embeddings for the document.", ErrorCategory.AI),
    RAG_RETRIEVAL_FAILED("ERR-AI-004", "Failed to retrieve relevant context for RAG processing.", ErrorCategory.AI);

    private final String code;
    private final String defaultMessage;
    private final ErrorCategory category;

    ErrorCode(String code, String defaultMessage, ErrorCategory category) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.category = category;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public ErrorCategory getCategory() {
        return category;
    }
}
