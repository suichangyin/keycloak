package org.keycloak.federation.rest.model;

public class AttributeConfigurations {
    private String parentAttr;
    private String countAttr;
    private String usernameAttr;
    private String passwordAttr;
    private String firstNameAttr;
    private String lastNameAttr;
    private String emailAttr;
    private String createTimeAttr;
    private String modifyTimeAttr;
    private String enabledAttr;
    private String emailVerifiedAttr;
    private String groupAttr;

    public AttributeConfigurations() {
    }

    public AttributeConfigurations(String parentAttr, String countAttr, String usernameAttr,
                                   String passwordAttr, String firstNameAttr, String lastNameAttr,
                                   String emailAttr, String createDateAttr, String modifyDateAttr,
                                   String enabledAttr, String emailVerifiedAttr, String groupAttr) {
        this.parentAttr = parentAttr;
        this.countAttr = countAttr;
        this.usernameAttr = usernameAttr;
        this.passwordAttr = passwordAttr;
        this.firstNameAttr = firstNameAttr;
        this.lastNameAttr = lastNameAttr;
        this.emailAttr = emailAttr;
        this.createTimeAttr = createDateAttr;
        this.modifyTimeAttr = modifyDateAttr;
        this.enabledAttr = enabledAttr;
        this.emailVerifiedAttr = emailVerifiedAttr;
        this.groupAttr = groupAttr;
    }

    public String getParentAttr() {
        return parentAttr;
    }

    public void setParentAttr(String parentAttrs) {
        this.parentAttr = parentAttrs;
    }

    public String getCountAttr() {
        return countAttr;
    }

    public void setCountAttr(String countAttr) {
        this.countAttr = countAttr;
    }

    public String getUsernameAttr() {
        return usernameAttr;
    }

    public void setUsernameAttr(String usernameAttr) {
        this.usernameAttr = usernameAttr;
    }

    public String getPasswordAttr() {
        return passwordAttr;
    }

    public void setPasswordAttr(String passwordAttr) {
        this.passwordAttr = passwordAttr;
    }

    public String getFirstNameAttr() {
        return firstNameAttr;
    }

    public void setFirstNameAttr(String firstNameAttr) {
        this.firstNameAttr = firstNameAttr;
    }

    public String getLastNameAttr() {
        return lastNameAttr;
    }

    public void setLastNameAttr(String lastNameAttr) {
        this.lastNameAttr = lastNameAttr;
    }

    public String getEmailAttr() {
        return emailAttr;
    }

    public void setEmailAttr(String emailAttr) {
        this.emailAttr = emailAttr;
    }

    public String getGroupAttr() {
        return groupAttr;
    }

    public void setGroupAttr(String groupAttr) {
        this.groupAttr = groupAttr;
    }

    public String getCreateTimeAttr() {
        return createTimeAttr;
    }

    public void setCreateTimeAttr(String createTimeAttr) {
        this.createTimeAttr = createTimeAttr;
    }

    public String getModifyTimeAttr() {
        return modifyTimeAttr;
    }

    public void setModifyTimeAttr(String modifyTimeAttr) {
        this.modifyTimeAttr = modifyTimeAttr;
    }

    public String getEnabledAttr() {
        return enabledAttr;
    }

    public void setEnabledAttr(String enabledAttr) {
        this.enabledAttr = enabledAttr;
    }

    public String getEmailVerifiedAttr() {
        return emailVerifiedAttr;
    }

    public void setEmailVerifiedAttr(String emailVerifiedAttr) {
        this.emailVerifiedAttr = emailVerifiedAttr;
    }
}
