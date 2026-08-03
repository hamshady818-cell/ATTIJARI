package com.awb.ged.common.exception;

import java.util.Map;

/**
 * <h1>StorageException</h1>
 * <p>
 * Technical exception representing a physical storage operation failure (IO errors, MinIO failures).
 * </p>
 */
public class StorageException extends TechnicalException {

    public StorageException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public StorageException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause, null);
    }

    public StorageException(ErrorCode errorCode, String message, Throwable cause, Map<String, Object> details) {
        super(errorCode, message, cause, details);
    }
}
