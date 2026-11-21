package com.itsm.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Turns every failure into a consistent JSON body: {status, error, message, timestamp}. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({BadRequestException.class, InvalidStateTransitionException.class})
    public ResponseEntity<Map<String, Object>> badRequest(RuntimeException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({DuplicateResourceException.class, TechnicianUnavailableException.class,
            WorkloadExceededException.class})
    public ResponseEntity<Map<String, Object>> conflict(RuntimeException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> constraint(ConstraintViolationException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> malformed(Exception ex) {
        String message = ex instanceof MethodArgumentTypeMismatchException m
                ? "Invalid value '" + m.getValue() + "' for " + m.getName()
                : ex instanceof HttpMessageNotReadableException
                ? "Request body is missing or malformed"
                : ex.getMessage();
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> badCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Incorrect username or password");
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> disabled(DisabledException ex) {
        return build(HttpStatus.UNAUTHORIZED, "This account has been deactivated. Contact your administrator.");
    }

    @ExceptionHandler(com.itsm.chatbot.ChatRateLimitException.class)
    public ResponseEntity<Map<String, Object>> chatRateLimit(com.itsm.chatbot.ChatRateLimitException ex) {
        return build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> accessDenied(AccessDeniedException ex) {
        String msg = ex.getMessage() == null || ex.getMessage().startsWith("Access Denied")
                ? "You do not have permission to do this" : ex.getMessage();
        return build(HttpStatus.FORBIDDEN, msg);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return build(HttpStatus.CONFLICT, "This change conflicts with existing data");
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> database(DataAccessException ex) {
        log.error("Database failure", ex);
        return build(HttpStatus.SERVICE_UNAVAILABLE, "The database is unavailable. Please try again shortly.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on the server");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now().toString());
        return new ResponseEntity<>(body, status);
    }
}
