package org.keycloak.providers.email.authentication.authenticators.browser;

public class EmailConstants {
    private EmailConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static String EMAIL_ADDRESS = "emailAddress";
    public static String CODE = "emailCode";
    public static String CODE_LENGTH = "length";
    public static String CODE_TTL = "ttl";
    public static int DEFAULT_LENGTH = 6;
    public static int DEFAULT_TTL = 300;
}
