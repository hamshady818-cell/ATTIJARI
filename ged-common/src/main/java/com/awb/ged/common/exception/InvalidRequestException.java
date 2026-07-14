package com.awb.ged.common.exception;

import java.util.Map;

/**
 * <h1>InvalidRequestException</h1>
 * <p>
 * Business exception representing a client request that failed validation rules or contained malformed data.
 * This will be mapped to an HTTP 400 Status in the API layer.
 * </p>
 */
public class InvalidRequestException extends BusinessException {

    /**
     * Constructs an InvalidRequestException with the specified error code.
     *
     * @param errorCode the stable error code representing the business error
     */
    public InvalidRequestException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * Constructs an InvalidRequestException with the specified error code and a custom message.
     *
     * @param errorCode the stable error code representing the business error
     * @param message   a custom description of the error
     */
    public InvalidRequestException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Constructs an InvalidRequestException with the specified error code, custom message, and details.
     *
     * @param errorCode the stable error code representing the business error
     * @param message   a custom description of the error
     * @param details   additional validation or context details (e.g. fields that failed validation)
     */
    public InvalidRequestException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(errorCode, message, details);
    }
}
