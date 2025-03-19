package org.keycloak.federation.rest.model;

public class ProviderConfig {

    private ConnConfigurations connConfigurations;
    private ExtraConfigurations extraConfigurations;
    private AttributeConfigurations attributeConfigurations;

    public ProviderConfig() {
    }

    public ProviderConfig(ConnConfigurations connConfigurations,
                          ExtraConfigurations extraConfigurations,
                          AttributeConfigurations attributeConfigurations) {
        this.connConfigurations = connConfigurations;
        this.extraConfigurations = extraConfigurations;
        this.attributeConfigurations = attributeConfigurations;
    }

    public ConnConfigurations getConnConfigurations() {
        return connConfigurations;
    }

    public void setConnConfigurations(ConnConfigurations connConfigurations) {
        this.connConfigurations = connConfigurations;
    }

    public ExtraConfigurations getExtraConfigurations() {
        return extraConfigurations;
    }

    public void setExtraConfigurations(ExtraConfigurations extraConfigurations) {
        this.extraConfigurations = extraConfigurations;
    }

    public AttributeConfigurations getAttributeConfigurations() {
        return attributeConfigurations;
    }

    public void setAttributeConfigurations(AttributeConfigurations attributeConfigurations) {
        this.attributeConfigurations = attributeConfigurations;
    }
}
