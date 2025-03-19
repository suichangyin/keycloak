package org.keycloak.federation.rest.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class RestUser {
    AttributeConfigurations config;
    private Map<String, Object> properties;

    public RestUser(AttributeConfigurations config, Map<String, Object> properties) {
        this.properties = properties;
        this.config = config;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public String getUserName() {
        return (String) this.properties.get(config.getUsernameAttr());
    }


    public String getFirstName() {
        return (String) this.properties.getOrDefault(config.getFirstNameAttr(), "");
    }

    public String getLastName() {
        return (String) this.properties.getOrDefault(config.getLastNameAttr(), "");
    }

    public String getEmail() {
        return (String) this.properties.getOrDefault(config.getEmailAttr(), "");
    }

    public boolean isEnabled() {
        return (Boolean) this.properties.getOrDefault(config.getEnabledAttr(), true);
    }

    public boolean isEmailVerified() {
        return (Boolean) this.properties.getOrDefault(config.getEmailVerifiedAttr(), false);
    }

    public Set<String> getRoles() {
        return Collections.emptySet();
    }

    public Map<String, List<String>> getAttributes() {
        return Collections.emptyMap();
    }

    public Set<String> getActions() {
        return Collections.emptySet();
    }

    public String getGroup() {
        return (String) this.properties.getOrDefault(config.getGroupAttr(), "");
    }

    public String getPassword() {
        return (String) this.properties.getOrDefault(config.getUsernameAttr(), "");
    }

    @Override
    public String toString() {
        return "RestUser{" +
                "username=" + getUserName() +
                ", password=" + getPassword() +
                ", firstName=" + getFirstName() +
                ", lastName=" + getLastName() +
                ", email=" + getEmail() +
                ", createTime=" + getEmail() +
                ", modifyTime=" + getPassword() +
                ", enabled=" + isEnabled() +
                ", emailVerified=" + isEmailVerified() +
                ", group=" + getGroup() +
                '}';
    }
}
