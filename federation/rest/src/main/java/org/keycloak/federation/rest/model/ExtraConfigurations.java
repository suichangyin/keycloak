package org.keycloak.federation.rest.model;

public class ExtraConfigurations {
    private Boolean attrSync;
    private Boolean roleSync;
    private Boolean passwordSync;
    private HashAlgorithm passwordHashAlgorithm;
    private Integer passwordHashIteration;
    private String roleClient;
    private String prefix;
    private Boolean uppercase;
    private Boolean uncheckFederation;
    private Boolean notCreateUsers;

    private Boolean autoLinkedIdentityProvider;
    private String identityProviderAlias;

    public ExtraConfigurations(Boolean attrSync, Boolean roleSync, Boolean passwordSync,
                               HashAlgorithm passwordHashAlgorithm, Integer passwordHashIteration,
                               String roleClient, String prefix, Boolean uppercase,
                               Boolean uncheckFederation, Boolean notCreateUsers,
                               Boolean autoLinkedIdentityProvider, String identityProviderAlias) {
        this.attrSync = attrSync;
        this.roleSync = roleSync;
        this.passwordSync = passwordSync;
        this.passwordHashAlgorithm = passwordHashAlgorithm;
        this.passwordHashIteration = passwordHashIteration;
        this.roleClient = roleClient;
        this.prefix = prefix;
        this.uppercase = uppercase;
        this.uncheckFederation = uncheckFederation;
        this.notCreateUsers = notCreateUsers;
        this.autoLinkedIdentityProvider = autoLinkedIdentityProvider;
        this.identityProviderAlias = identityProviderAlias;
    }

    public Boolean getAttrSync() {
        return attrSync;
    }

    public void setAttrSync(Boolean attrSync) {
        this.attrSync = attrSync;
    }

    public Boolean getRoleSync() {
        return roleSync;
    }

    public void setRoleSync(Boolean roleSync) {
        this.roleSync = roleSync;
    }

    public Boolean getPasswordSync() {
        return passwordSync;
    }

    public void setPasswordSync(Boolean passwordSync) {
        this.passwordSync = passwordSync;
    }

    public HashAlgorithm getPasswordHashAlgorithm() {
        return passwordHashAlgorithm;
    }

    public void setPasswordHashAlgorithm(HashAlgorithm passwordHashAlgorithm) {
        this.passwordHashAlgorithm = passwordHashAlgorithm;
    }

    public Integer getPasswordHashIteration() {
        return passwordHashIteration;
    }

    public void setPasswordHashIteration(Integer passwordHashIteration) {
        this.passwordHashIteration = passwordHashIteration;
    }

    public String getRoleClient() {
        return roleClient;
    }

    public void setRoleClient(String roleClient) {
        this.roleClient = roleClient;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Boolean getUppercase() {
        return uppercase;
    }

    public void setUppercase(Boolean uppercase) {
        this.uppercase = uppercase;
    }

    public Boolean getUncheckFederation() {
        return uncheckFederation;
    }

    public void setUncheckFederation(Boolean uncheckFederation) {
        this.uncheckFederation = uncheckFederation;
    }

    public Boolean is_not_create_users() {
        return notCreateUsers;
    }

    public void set_not_create_users(Boolean not_create_users) {
        this.notCreateUsers = not_create_users;
    }

    public Boolean isAutoLinkedIdentityProvider() {
        return autoLinkedIdentityProvider;
    }

    public void setAutoLinkedIdentityProvider(Boolean autoLinkedIdentityProvider) {
        this.autoLinkedIdentityProvider = autoLinkedIdentityProvider;
    }

    public String getIdentityProviderAlias() {
        return identityProviderAlias;
    }

    public void setIdentityProviderAlias(String identityProviderAlias) {
        this.identityProviderAlias = identityProviderAlias;
    }
}
