package org.keycloak.federation.rest.model;


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum HashAlgorithm {
    SHA256("SHA256", "SHA256"),
    PBKDF2_SHA256("PBKDF2-SHA256", "PBKDF2-SHA256");

    private final String name;
    private final String desc;

    HashAlgorithm(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public static HashAlgorithm getByDescription(String desc) {
        for (HashAlgorithm value : values()) {
            if (value.desc.equals(desc)) {
                return value;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public static List<String> getAllDescriptions() {
        return Arrays.stream(values()).map(HashAlgorithm::getDesc).collect(Collectors.toList());
    }
}
