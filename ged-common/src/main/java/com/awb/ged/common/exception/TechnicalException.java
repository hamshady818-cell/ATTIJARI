package com.awb.ged.common.exception;

import java.util.Map;

/**
 * <h1>TechnicalException</h1>
 * <p>
 * Exception representing unexpected system, storage, network, or infra failures.
 * This will be mapped to an HTTP 500 Status in the API layer.
 * </p>
 */
public class TechnicalException extends BusinessException {

    /**
     * Constructs a TechnicalException with the specified error code.
     *
     * @param errorCode the stable error code representing the business error
     */
    public TechnicalException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * Constructs a TechnicalException with the specified error code and a custom message.
     *
     * @param errorCode the stable error code representing the business error
     * @param message   a custom description of the error
     */
    public TechnicalException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Constructs a TechnicalException with the specified error code, custom message, and details.
     *
     * @param errorCode the stable error code representing the business error
     * @param message   a custom description of the error
     * @param details   additional technical details
     */
    public TechnicalException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(errorCode, message, details);
    }

    /**
     * Constructs a TechnicalException with the specified error code, custom message, cause, and details.
     *
     * @param errorCode the stable error code representing the business error
     * @param message   a custom description of the error
     * @param cause     the underlying exception
     * @param details   additional technical details
     */
    public TechnicalException(ErrorCode errorCode, String message, Throwable cause, Map<String, Object> details) {
        super(errorCode, message, cause, details);
    }
}
