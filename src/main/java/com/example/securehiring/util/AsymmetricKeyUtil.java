package com.example.securehiring.util;

import org.springframework.stereotype.Component;

import java.io.*;
import java.security.*;

@Component
public class AsymmetricKeyUtil {

    private static final String ALGORITHM = "RSA";
    private static final int KEY_SIZE = 1024;

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(ALGORITHM);
        keyPairGen.initialize(KEY_SIZE);

        return keyPairGen.generateKeyPair();
    }

    public static void savePublicKey(String fname, PublicKey publicKey) throws IOException {
        try (FileOutputStream fstream = new FileOutputStream(fname);
             ObjectOutputStream ostream = new ObjectOutputStream(fstream)) {
            ostream.writeObject(publicKey);
        }
    }

    public static void savePrivateKey(String fname, PrivateKey privateKey) throws IOException {
        try(FileOutputStream fstream = new FileOutputStream(fname);
            ObjectOutputStream ostream = new ObjectOutputStream(fstream)) {
                ostream.writeObject(privateKey);
        }
    }

    public static PublicKey loadPublicKey(String fname) throws IOException, ClassNotFoundException {
        try (FileInputStream fis = new FileInputStream(fname);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            Object obj = ois.readObject();
            return (PublicKey)obj;
        }
    }

    public static PrivateKey loadPrivateKey(String fname) throws IOException, ClassNotFoundException{
        try (FileInputStream fis = new FileInputStream(fname);
            ObjectInputStream ois = new ObjectInputStream(fis)) {
            Object obj = ois.readObject();
            return (PrivateKey)obj;
        }
    }
}
