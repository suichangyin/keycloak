package org.keycloak.federation.rest.model;


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum SignAlgorithm {
    MD5_BASE64("MD5_BASE64", "MD5 and Base64");

    private final String name;
    private final String desc;

    SignAlgorithm(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public static SignAlgorithm getByDescription(String desc) {
        for (SignAlgorithm value : values()) {
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
        return Arrays.stream(values()).map(SignAlgorithm::getDesc).collect(Collectors.toList());
    }
}
