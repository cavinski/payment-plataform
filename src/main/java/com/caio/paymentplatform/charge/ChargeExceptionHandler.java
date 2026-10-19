package com.caio.paymentplatform.charge;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.caio.paymentplatform.charge.exceptions.ChargeNotFoundException;

import java.util.Map;

@RestControllerAdvice 
public class ChargeExceptionHandler {
 
    @ExceptionHandler(ChargeNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ChargeNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
            "code", "CHARGE_NOT_FOUND",
            "message", exception.getMessage()
        ));
    }
}