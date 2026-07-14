package com.awb.ged.common.exception;

import java.util.Map;

/**
 * <h1>ForbiddenException</h1>
 * <p>
 * Business exception representing an authenticated request that lacks sufficient permissions to access a resource.
 * This will be mapped to an HTTP 403 Status in the API layer.
 * </p>
 */
public class ForbiddenException extends BusinessException {

    /**
     * Constructs a ForbiddenException with the specified error code.
     *
     * @param errorCode the stable error code representing the business error
     */
    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * Constructs a ForbiddenException with the specified error code and a custom message.
     *
     * @param errorCode the stable error code representing the business error
     * @param message   a custom description of the error
     */
    public ForbiddenException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Constructs a ForbiddenException with the specified error code, custom message, and details.
     *
     * @param errorCode the stable error code representing the business error
     * @param message   a custom description of the error
     * @param details   additional context details
     */
    public ForbiddenException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(errorCode, message, details);
    }
}
