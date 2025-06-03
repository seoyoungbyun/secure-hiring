package com.example.securehiring.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class CryptoException extends RuntimeException {
    public CryptoException(String message) {
        super(message);
    }
    public CryptoException(String message, Throwable cause) {super(message, cause);}
}