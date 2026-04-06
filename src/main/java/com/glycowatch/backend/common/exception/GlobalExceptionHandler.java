package com.glycowatch.backend.common.exception;

import com.glycowatch.backend.common.dto.response.ErrorResponse;
import com.glycowatch.backend.common.dto.response.ValidationErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus())
                .body(buildErrorResponse(ex.getErrorCode(), ex.getMessage(), request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ValidationErrorResponse.FieldViolation> violations = new ArrayList<>();
        violations.addAll(ex.getBindingResult().getFieldErrors().stream().map(this::toFieldViolation).toList());
        violations.addAll(ex.getBindingResult().getGlobalErrors().stream().map(this::toGlobalViolation).toList());

        return ResponseEntity.badRequest()
                .body(buildValidationErrorResponse("Request validation failed.", violations, request));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ValidationErrorResponse> handleBindException(
            BindException ex,
            HttpServletRequest request
    ) {
        List<ValidationErrorResponse.FieldViolation> violations = new ArrayList<>();
        violations.addAll(ex.getBindingResult().getFieldErrors().stream().map(this::toFieldViolation).toList());
        violations.addAll(ex.getBindingResult().getGlobalErrors().stream().map(this::toGlobalViolation).toList());

        return ResponseEntity.badRequest()
                .body(buildValidationErrorResponse("Request validation failed.", violations, request));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleHandlerMethodValidation(
            HandlerMethodValidationException ex,
            HttpServletRequest request
    ) {
        List<ValidationErrorResponse.FieldViolation> violations = ex.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> ValidationErrorResponse.FieldViolation.builder()
                                .field(resolveParameterName(result))
                                .message(error.getDefaultMessage())
                                .build()))
                .toList();

        return ResponseEntity.badRequest()
                .body(buildValidationErrorResponse("Request validation failed.", violations, request));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedRequest(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest()
                .body(buildErrorResponse("MALFORMED_REQUEST", "Malformed JSON request.", request));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        List<ValidationErrorResponse.FieldViolation> violations = ex.getConstraintViolations().stream()
                .map(this::toFieldViolation)
                .toList();

        return ResponseEntity.badRequest()
                .body(buildValidationErrorResponse("Request validation failed.", violations, request));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ValidationErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        ValidationErrorResponse.FieldViolation violation = ValidationErrorResponse.FieldViolation.builder()
                .field(ex.getName())
                .message("Parameter value is invalid.")
                .build();

        return ResponseEntity.badRequest()
                .body(buildValidationErrorResponse("Request validation failed.", List.of(violation), request));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ValidationErrorResponse> handleMissingRequestParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        ValidationErrorResponse.FieldViolation violation = ValidationErrorResponse.FieldViolation.builder()
                .field(ex.getParameterName())
                .message("Request parameter is required.")
                .build();

        return ResponseEntity.badRequest()
                .body(buildValidationErrorResponse("Request validation failed.", List.of(violation), request));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ValidationErrorResponse> handleMissingRequestHeader(
            MissingRequestHeaderException ex,
            HttpServletRequest request
    ) {
        ValidationErrorResponse.FieldViolation violation = ValidationErrorResponse.FieldViolation.builder()
                .field(ex.getHeaderName())
                .message("Request header is required.")
                .build();

        return ResponseEntity.badRequest()
                .body(buildValidationErrorResponse("Request validation failed.", List.of(violation), request));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildErrorResponse("DATA_INTEGRITY_ERROR", "A data integrity rule was violated.", request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse("INTERNAL_SERVER_ERROR", "An unexpected error occurred.", request));
    }

    private ValidationErrorResponse.FieldViolation toFieldViolation(FieldError fieldError) {
        return ValidationErrorResponse.FieldViolation.builder()
                .field(fieldError.getField())
                .message(fieldError.getDefaultMessage())
                .build();
    }

    private ValidationErrorResponse.FieldViolation toGlobalViolation(ObjectError objectError) {
        return ValidationErrorResponse.FieldViolation.builder()
                .field(objectError.getObjectName())
                .message(objectError.getDefaultMessage())
                .build();
    }

    private ValidationErrorResponse.FieldViolation toFieldViolation(ConstraintViolation<?> violation) {
        return ValidationErrorResponse.FieldViolation.builder()
                .field(extractLeafField(violation.getPropertyPath().toString()))
                .message(violation.getMessage())
                .build();
    }

    private ValidationErrorResponse buildValidationErrorResponse(
            String message,
            List<ValidationErrorResponse.FieldViolation> violations,
            HttpServletRequest request
    ) {
        return ValidationErrorResponse.builder()
                .success(false)
                .error("VALIDATION_ERROR")
                .message(message)
                .violations(violations)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
    }

    private ErrorResponse buildErrorResponse(String error, String message, HttpServletRequest request) {
        return ErrorResponse.builder()
                .success(false)
                .error(error)
                .message(message)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
    }

    private String extractLeafField(String propertyPath) {
        int lastDot = propertyPath.lastIndexOf('.');
        return lastDot >= 0 ? propertyPath.substring(lastDot + 1) : propertyPath;
    }

    private String resolveParameterName(org.springframework.validation.method.ParameterValidationResult result) {
        String parameterName = result.getMethodParameter().getParameterName();
        return parameterName != null ? parameterName : result.getMethodParameter().getParameter().getName();
    }
}
