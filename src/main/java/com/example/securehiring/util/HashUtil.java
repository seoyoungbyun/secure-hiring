package com.example.securehiring.util;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class HashUtil {

    private static String algorithm = "SHA-256";

    public static byte[] calcHashVal(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        return md.digest(data);
    }

//    public static boolean compareHashVal(byte[] h1, byte[] h2) {
//        return Arrays.equals(h1, h2);
//    }
}
