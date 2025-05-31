package com.example.securehiring.util;

import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;

@Component
public class AsymmetricKeyUtil {

    private static final Path keyDir = Paths.get("uploads/keys/");

    private static String algorithm = "RSA";
    private static int keySize = 1024;

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(algorithm);
        keyPairGen.initialize(keySize);

        return keyPairGen.generateKeyPair();
    }

    public static void savePublicKey(String fname, PublicKey publicKey) throws IOException {
        if (!Files.exists(keyDir)) {
            Files.createDirectories(keyDir); // 디렉토리가 없으면 생성
        }

        try (FileOutputStream fstream = new FileOutputStream(keyDir.toString() + "/" + fname);
             ObjectOutputStream ostream = new ObjectOutputStream(fstream)) {
            ostream.writeObject(publicKey);
        }
    }

    public static void savePrivateKey(String fname, PrivateKey privateKey) throws IOException {
        if (!Files.exists(keyDir)) {
            Files.createDirectories(keyDir); // 디렉토리가 없으면 생성
        }

        try(FileOutputStream fstream = new FileOutputStream(keyDir.toString() + "/" + fname);
            ObjectOutputStream ostream = new ObjectOutputStream(fstream)) {
                ostream.writeObject(privateKey);
        }
    }

    public static PublicKey loadPublicKey(String fname) throws IOException, ClassNotFoundException {
        try (FileInputStream fis = new FileInputStream(keyDir.toString() + "/" + fname);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            Object obj = ois.readObject();
            return (PublicKey)obj;
        }
    }

    public static PrivateKey loadPrivateKey(String fname) throws IOException, ClassNotFoundException{
        try (FileInputStream fis = new FileInputStream(keyDir.toString() + "/" + fname);
            ObjectInputStream ois = new ObjectInputStream(fis)) {
            Object obj = ois.readObject();
            return (PrivateKey)obj;
        }
    }
}
