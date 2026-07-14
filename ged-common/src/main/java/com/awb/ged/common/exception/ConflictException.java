package com.awb.ged.common.exception;

import java.util.Map;

/**
 * <h1>ConflictException</h1>
 * <p>
 * Business exception representing a state conflict or concurrency issue (e.g. duplicate document name, lock collision).
 * This will be mapped to an HTTP 409 Status in the API layer.
 * </p>
 */
public class ConflictException extends BusinessException {

    /**
     * Constructs a ConflictException with the specified error code.
     *
     * @param errorCode the stable error code representing the business error
     */
    public ConflictException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * Constructs a ConflictException with the specified error code and a custom message.
     *
     * @param errorCode the stable error code representing the business error
     * @param message   a custom description of the error
     */
    public ConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Constructs a ConflictException with the specified error code, custom message, and details.
     *
     * @param errorCode the stable error code representing the business error
     * @param message   a custom description of the error
     * @param details   additional context details (e.g. conflicting entity attributes)
     */
    public ConflictException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(errorCode, message, details);
    }
}
