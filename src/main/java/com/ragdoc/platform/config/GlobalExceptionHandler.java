package com.ragdoc.platform.config;

import com.ragdoc.platform.common.MessageKeys;
import com.ragdoc.platform.common.MessageResolver;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * 전역 예외 처리기.
 * 모든 API 에러는 {@code { "message": "..." }} 형식으로 반환하며, Accept-Language에 따라 i18n된다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageResolver messageResolver;

    public GlobalExceptionHandler(MessageResolver messageResolver) {
        this.messageResolver = messageResolver;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null
                ? resolveValidationMessage(fieldError.getDefaultMessage())
                : messageResolver.resolve(MessageKeys.COMMON_INVALID_INPUT);
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    private String resolveValidationMessage(String rawMessage) {
        if (rawMessage == null) {
            return messageResolver.resolve(MessageKeys.COMMON_INVALID_INPUT);
        }

        // Jakarta Validation 메시지 키 형식: "{validation.email.required}" → 키만 추출해 번역
        if (rawMessage.startsWith("{") && rawMessage.endsWith("}")) {
            return messageResolver.resolve(rawMessage.substring(1, rawMessage.length() - 1));
        }

        return messageResolver.resolve(rawMessage);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", messageResolver.resolve(ex.getMessage())));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        String message = ex.getReason() != null
                ? messageResolver.resolve(ex.getReason())
                : messageResolver.resolve(MessageKeys.COMMON_REQUEST_FAILED);
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", message));
    }
}
