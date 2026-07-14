package com.awb.ged.common.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <h1>BusinessException</h1>
 * <p>
 * Base exception for all business and functional validation errors in the GED-AWB system.
 * This class is designed to be completely independent of any web framework (such as Spring),
 * carrying only pure business context like {@link ErrorCode} and a map of arbitrary details.
 * </p>
 * <p>
 * Mapping to HTTP status codes should be handled downstream in the API layer.
 * </p>
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    /**
     * Constructs a BusinessException with the specified error code and a default message.
     *
     * @param errorCode the stable error code representing the business error
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.details = Collections.emptyMap();
    }

    /**
     * Constructs a BusinessException with the specified error code and a custom message.
     *
     * @param errorCode the stable error code representing the business error
     * @param message   a custom description of the error
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = Collections.emptyMap();
    }

    /**
     * Constructs a BusinessException with the specified error code, custom message, and details.
     *
     * @param errorCode the stable error code representing the business error
     * @param message   a custom description of the error
     * @param details   additional context details about the error (e.g. invalid fields, resource IDs)
     */
    public BusinessException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details != null ? Collections.unmodifiableMap(new LinkedHashMap<>(details)) : Collections.emptyMap();
    }

    /**
     * Constructs a BusinessException with the specified error code, custom message, cause, and details.
     *
     * @param errorCode the stable error code representing the business error
     * @param message   a custom description of the error
     * @param cause     the underlying technical cause of this exception
     * @param details   additional context details about the error
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause, Map<String, Object> details) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = details != null ? Collections.unmodifiableMap(new LinkedHashMap<>(details)) : Collections.emptyMap();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
