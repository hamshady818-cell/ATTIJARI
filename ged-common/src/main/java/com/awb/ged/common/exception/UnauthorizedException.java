package com.awb.ged.common.exception;

import java.util.Map;

/**
 * <h1>UnauthorizedException</h1>
 * <p>
 * Business exception representing an unauthenticated request (e.g. invalid or expired Keycloak token).
 * This will be mapped to an HTTP 401 Status in the API layer.
 * </p>
 */
public class UnauthorizedException extends BusinessException {

    /**
     * Constructs an UnauthorizedException with the specified error code.
     *
     * @param errorCode the stable error code representing the business error
     */
    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * Constructs an UnauthorizedException with the specified error code and a custom message.
     *
     * @param errorCode the stable error code representing the business error
     * @param message   a custom description of the error
     */
    public UnauthorizedException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Constructs an UnauthorizedException with the specified error code, custom message, and details.
     *
     * @param errorCode the stable error code representing the business error
     * @param message   a custom description of the error
     * @param details   additional context details
     */
    public UnauthorizedException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(errorCode, message, details);
    }
}
