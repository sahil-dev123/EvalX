package com.evalx.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {

    /**
     * Normalizes text by removing extra whitespaces, newlines, and making it lowercase
     * to safely handle PDF extraction inconsistencies before hashing.
     */
    public static String normalizeText(String text) {
        if (text == null) return "";
        return text.replaceAll("\\s+", " ").trim().toLowerCase();
    }

    /**
     * Generates a SHA-256 hash of the normalized text.
     */
    public static String generateHash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(normalizeText(text).getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
