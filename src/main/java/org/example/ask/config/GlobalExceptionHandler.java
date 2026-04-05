package org.example.ask.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice(basePackages = "org.example.ask")
public class GlobalExceptionHandler {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static String requestInfo(HttpServletRequest req) {
        String qs = req.getQueryString();
        return req.getMethod() + " " + req.getRequestURI() + (qs != null ? "?" + qs : "");
    }

    // -----------------------------------------------------------------------
    // Handlers
    // -----------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex,
                                                                HttpServletRequest req) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("[{}] Validation failed: {}", requestInfo(req), errors);
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex,
                                                                     HttpServletRequest req) {
        log.warn("[{}] Illegal argument: {}", requestInfo(req), ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex,
                                                                    HttpServletRequest req) {
        log.warn("[{}] Bad credentials: {}", requestInfo(req), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSize(MaxUploadSizeExceededException ex,
                                                                   HttpServletRequest req) {
        log.warn("[{}] Upload size exceeded: {}", requestInfo(req), ex.getMessage());
        return ResponseEntity.status(413)
                .body(Map.of("error", "File size exceeds the maximum allowed limit"));
    }

    /** Catch-all — logs the full stack trace so it always appears in the terminal. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex, HttpServletRequest req) {
        log.error("[{}] Unhandled exception — {}: {}",
                requestInfo(req), ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return ResponseEntity.internalServerError()
                .body(Map.of("error", "An unexpected error occurred"));
    }
}

