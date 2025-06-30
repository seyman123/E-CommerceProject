package com.seyman.dreamshops.exceptions;

import com.seyman.dreamshops.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDeniedException(AccessDeniedException exception) {
        String message = "Bu işlem için yetkiniz bulunmamaktadır";
        return new ResponseEntity<>(message, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        // İlk hatayı ana mesaj olarak kullan
        String mainMessage = errors.isEmpty() ? "Validation hatası" : errors.values().iterator().next();
        
        return ResponseEntity.badRequest()
                .body(new ApiResponse(mainMessage, errors));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(ex.getMessage(), null));
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<ApiResponse> handleAlreadyExistsException(AlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse(ex.getMessage(), null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse(ex.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGenericException(Exception ex) {
        // Skip logging for common browser requests that don't need to be logged
        if (ex.getMessage() != null && ex.getMessage().contains("favicon.ico")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse("Resource not found", null));
        }
        
        // Log the actual exception for debugging
        System.err.println("=== GLOBAL EXCEPTION HANDLER ===");
        System.err.println("Exception Type: " + ex.getClass().getSimpleName());
        System.err.println("Exception Message: " + ex.getMessage());
        ex.printStackTrace();
        System.err.println("=== END EXCEPTION DETAILS ===");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse("Sunucu hatası oluştu. Lütfen daha sonra tekrar deneyin.", null));
    }
}
