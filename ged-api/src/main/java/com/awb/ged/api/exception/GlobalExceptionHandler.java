package com.awb.ged.api.exception;

import com.awb.ged.common.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getErrorCode().getCode(), ex.getMessage(), ex.getDetails());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getErrorCode().getCode(), ex.getMessage(), ex.getDetails());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(ForbiddenException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getErrorCode().getCode(), ex.getMessage(), ex.getDetails());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN.getCode(), "Access is denied.", null);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getErrorCode().getCode(), ex.getMessage(), ex.getDetails());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getCode(), "Authentication required.", null);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(InvalidRequestException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getErrorCode().getCode(), ex.getMessage(), ex.getDetails());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_INPUT.getCode(),
                "Validation failed for request parameters.",
                errors
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return buildResponse(
                HttpStatus.PAYLOAD_TOO_LARGE,
                ErrorCode.FILE_TOO_LARGE.getCode(),
                "Uploaded file exceeds maximum allowed size.",
                null
        );
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingPart(MissingServletRequestPartException ex) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_INPUT.getCode(),
                "Required request part '" + ex.getRequestPartName() + "' is missing.",
                null
        );
    }

    @ExceptionHandler({StorageException.class, TechnicalException.class})
    public ResponseEntity<ApiErrorResponse> handleTechnical(TechnicalException ex) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getErrorCode().getCode(),
                ex.getMessage(),
                ex.getDetails()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(Exception ex) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR.getCode(),
                "An unexpected internal error occurred: " + ex.getMessage(),
                null
        );
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String code, String message, Map<String, Object> details) {
        ApiErrorResponse body = ApiErrorResponse.builder()
                .code(code)
                .message(message)
                .timestamp(Instant.now())
                .details(details)
                .build();

        return ResponseEntity.status(status).body(body);
    }
}
