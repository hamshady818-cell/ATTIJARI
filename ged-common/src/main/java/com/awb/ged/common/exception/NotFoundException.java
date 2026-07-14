package com.awb.ged.common.exception;

import java.util.Map;

/**
 * <h1>NotFoundException</h1>
 * <p>
 * Business exception representing a "Resource Not Found" situation (e.g. document, folder, or user does not exist).
 * This will be mapped to an HTTP 404 Status in the API layer.
 * </p>
 */
public class NotFoundException extends BusinessException {

    /**
     * Constructs a NotFoundException with the specified error code.
     *
     * @param errorCode the stable error code representing the business error
     */
    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * Constructs a NotFoundException with the specified error code and a custom message.
     *
     * @param errorCode the stable error code representing the business error
     * @param message   a custom description of the error
     */
    public NotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Constructs a NotFoundException with the specified error code, custom message, and details.
     *
     * @param errorCode the stable error code representing the business error
     * @param message   a custom description of the error
     * @param details   additional context details (e.g. resource ID, search criteria)
     */
    public NotFoundException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(errorCode, message, details);
    }
}
