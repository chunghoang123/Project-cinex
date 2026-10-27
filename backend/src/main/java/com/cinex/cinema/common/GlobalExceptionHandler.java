package com.cinex.cinema.common;

import lombok.extern.slf4j.Slf4j;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
            errors.put(err.getField(), err.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Dữ liệu không hợp lệ", errors));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
            .body(ApiResponse.error(ex.getReason()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Lỗi ràng buộc dữ liệu: " + ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Vi phạm toàn vẹn dữ liệu: " + ex.getMostSpecificCause().getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("Tên đăng nhập hoặc mật khẩu không đúng, hoặc tài khoản đã bị khóa"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Dữ liệu gửi lên không đúng định dạng JSON"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Thiếu tham số bắt buộc: " + ex.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Tham số " + ex.getName() + " không đúng định dạng"));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ApiResponse<Void>> handleDateParse(DateTimeParseException ex) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Giá trị ngày giờ không đúng định dạng (yyyy-MM-ddTHH:mm:ss): " + ex.getParsedString()));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(
            org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("Không tìm thấy endpoint: " + ex.getResourcePath()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(ApiResponse.error("Phương thức " + ex.getMethod() + " không được hỗ trợ cho endpoint này"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Lỗi hệ thống chưa được xử lý", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("Lỗi hệ thống: " + ex.getMessage()));
    }
}