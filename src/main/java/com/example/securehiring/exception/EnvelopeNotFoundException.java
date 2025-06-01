package com.example.securehiring.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class EnvelopeNotFoundException extends RuntimeException {
    public EnvelopeNotFoundException(String message) {
        super(message);
    }
    public EnvelopeNotFoundException(String message, Throwable cause) {super(message, cause);}
}
