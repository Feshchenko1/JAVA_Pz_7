package com.dailycodework.pz_4_1.exception.handler;

import com.dailycodework.pz_4_1.payload.response.MessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        // Можна повернути MessageResponse або більш детальний Map
        // Для тесту, який очікує "$.message", ми адаптуємо відповідь
        // Або можемо просто повернути errors
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    // Якщо ви очікуєте MessageResponse, то потрібно адаптувати:
    @ExceptionHandler(RuntimeException.class) // Це може бути ваш Error: Username/Email already taken
    public ResponseEntity<MessageResponse> handleRuntimeExceptions(RuntimeException ex) {
        return ResponseEntity
                .badRequest()
                .body(new MessageResponse(ex.getMessage()));
    }
}