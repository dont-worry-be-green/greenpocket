package com.greenpocket.global.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.greenpocket.global.response.ApiError;
import com.greenpocket.global.response.ApiResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
		ErrorCode errorCode = exception.getErrorCode();
		ApiError error = ApiError.of(errorCode, exception.getField(), exception.getDetails());
		return ResponseEntity.status(errorCode.status()).body(ApiResponse.failure(error));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
		MethodArgumentNotValidException exception
	) {
		FieldError fieldError = exception.getBindingResult().getFieldErrors().stream()
			.findFirst()
			.orElse(null);
		String field = fieldError == null ? null : fieldError.getField();
		return invalidRequest(field);
	}

	@ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class})
	public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(Exception exception) {
		return invalidRequest(null);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
		log.error("Unhandled server exception", exception);
		CommonErrorCode errorCode = CommonErrorCode.INTERNAL_ERROR;
		return ResponseEntity.status(errorCode.status())
			.body(ApiResponse.failure(ApiError.of(errorCode)));
	}

	private ResponseEntity<ApiResponse<Void>> invalidRequest(String field) {
		CommonErrorCode errorCode = CommonErrorCode.INVALID_REQUEST;
		ApiError error = ApiError.of(errorCode, field, null);
		return ResponseEntity.status(errorCode.status()).body(ApiResponse.failure(error));
	}
}
