package com.example.securehiring.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class CompanyNotFoundException extends RuntimeException {
    public CompanyNotFoundException(String message) {
        super(message);
    }
    public CompanyNotFoundException(String message, Throwable cause) {super(message, cause);}
}
