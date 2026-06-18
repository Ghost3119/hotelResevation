package com.hotelmanager.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        ApiError error = baseError(HttpStatus.BAD_REQUEST, null, "Validation failed", req);
        List<FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new FieldError(f.getField(), f.getDefaultMessage()))
                .toList();
        error.setFieldErrors(fields);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        ApiError error = baseError(HttpStatus.BAD_REQUEST, null, "Constraint violation", req);
        List<FieldError> fields = ex.getConstraintViolations().stream()
                .map(cv -> new FieldError(cv.getPropertyPath() != null ? cv.getPropertyPath().toString() : null, cv.getMessage()))
                .toList();
        error.setFieldErrors(fields);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest req) {
        ApiError error = baseError(ex.getStatus(), ex.getCode(), ex.getMessage(), req);
        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArg(IllegalArgumentException ex, HttpServletRequest req) {
        ApiError error = baseError(HttpStatus.BAD_REQUEST, null, ex.getMessage(), req);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(EntityNotFoundException ex, HttpServletRequest req) {
        ApiError error = baseError(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage(), req);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleForbidden(AccessDeniedException ex, HttpServletRequest req) {
        ApiError error = baseError(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Access denied", req);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        String sqlState = extractSqlState(ex);
        ErrorCode code;
        HttpStatus status;
        String message;
        if ("23P01".equals(sqlState)) {
            code = ErrorCode.RESERVATION_OVERLAP;
            status = HttpStatus.CONFLICT;
            message = "Room is already reserved for the requested date range";
        } else if ("23505".equals(sqlState)) {
            code = ErrorCode.CONFLICT;
            status = HttpStatus.CONFLICT;
            message = "A resource with the same unique key already exists";
        } else if ("23503".equals(sqlState)) {
            code = ErrorCode.RESOURCE_NOT_FOUND;
            status = HttpStatus.NOT_FOUND;
            message = "Referenced resource not found";
        } else {
            code = ErrorCode.CONFLICT;
            status = HttpStatus.CONFLICT;
            message = "Data integrity violation";
        }
        ApiError error = baseError(status, code, message, req);
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        ApiError error = baseError(HttpStatus.INTERNAL_SERVER_ERROR, null, ex.getMessage() != null ? ex.getMessage() : "Internal server error", req);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private ApiError baseError(HttpStatus status, ErrorCode code, String message, HttpServletRequest req) {
        ApiError error = new ApiError();
        error.setTimestamp(Instant.now());
        error.setStatus(status.value());
        error.setError(status.getReasonPhrase());
        error.setCode(code);
        error.setMessage(message);
        error.setPath(req.getRequestURI());
        return error;
    }

    private String extractSqlState(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            Object sqlState = tryGetField(current, "sqlState");
            if (sqlState instanceof String s && !s.isBlank()) {
                return s;
            }
            current = current.getCause();
        }
        return null;
    }

    private Object tryGetField(Object target, String fieldName) {
        try {
            Class<?> clazz = target.getClass();
            while (clazz != null) {
                try {
                    java.lang.reflect.Field f = clazz.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    return f.get(target);
                } catch (NoSuchFieldException ignored) {
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
