package com.example.securehiring.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class KeyProcessingException extends RuntimeException {
    public KeyProcessingException(String message) {
        super(message);
    }
    public KeyProcessingException(String message, Throwable cause) { super(message, cause); }
}
