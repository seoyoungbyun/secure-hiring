package com.example.securehiring.exception;

public class KeyProcessingException extends RuntimeException {
    public KeyProcessingException(String message) {super();}
    public KeyProcessingException(String message, Throwable cause) {
        super(message);
    }
}
