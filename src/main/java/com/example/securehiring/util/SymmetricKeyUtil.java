package com.example.securehiring.util;

import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

@Component
public class SymmetricKeyUtil {

    private static final Path keyDir = Paths.get("uploads/keys/");

    private static String algorithm = "AES";
    private static int keySize = 128;

    public static final String getAlgorithm() {
        return algorithm;
    }

    public static Key generateSecretKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(algorithm);
        keyGen.init(keySize);
        return keyGen.generateKey();
    }
}
