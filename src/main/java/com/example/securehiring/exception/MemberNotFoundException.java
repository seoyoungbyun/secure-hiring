package com.example.securehiring.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class MemberNotFoundException extends RuntimeException {
    public MemberNotFoundException(String message) {super(message);}
    public MemberNotFoundException(String message, Throwable cause) {super(message, cause);}
}
