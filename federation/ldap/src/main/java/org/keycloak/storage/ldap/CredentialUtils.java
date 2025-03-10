package org.keycloak.storage.ldap;

import org.keycloak.common.util.Base64;
import org.keycloak.common.util.Encode;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class CredentialUtils {

    public static String sshaHash(String password) {
        try {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            SecureRandom secureRandom = new SecureRandom();
            byte[] salt = new byte[4];
            secureRandom.nextBytes(salt);
            crypt.reset();
            crypt.update(password.getBytes());
            crypt.update(salt);
            byte[] hash = crypt.digest();
            byte[] hashPlusSalt = new byte[hash.length + salt.length];
            System.arraycopy(hash, 0, hashPlusSalt, 0, hash.length);
            System.arraycopy(salt, 0, hashPlusSalt, hash.length, salt.length);

            return new StringBuilder().append("{SSHA}").append(Base64.encodeBytes(hashPlusSalt)).toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    public static String ntlmHash(String password) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD4");
        } catch (NoSuchAlgorithmException e) {
            return "";
        }

        return Encode.hexString(md.digest(Encode.unicodeBytes(password)));
    }
}
