package net.kamradtfamily.blockchain.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CryptoUtil {
    /**
     * Create a hash from a string that consists of a hexadecimal number
     * in string form. Used throughout the blockchain ecosystem.
     * @param s The string to hash
     * @return the hash
     */
    public static String hash(String s) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("cypto error", e);
        }
        byte[] hash = digest.digest(
                s.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
