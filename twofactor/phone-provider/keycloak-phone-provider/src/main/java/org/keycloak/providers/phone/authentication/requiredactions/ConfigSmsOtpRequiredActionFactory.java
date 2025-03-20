package org.keycloak.providers.phone.authentication.requiredactions;

import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;

public class ConfigSmsOtpRequiredActionFactory extends ConfigSmsWechatOtpRequiredActionFactory {

    private static final ConfigSmsOtpRequiredAction instance = new ConfigSmsOtpRequiredAction();

    @Override
    public String getDisplayText() {
        return "Configure OTP over SMS";
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return instance;
    }

    @Override
    public String getId() {
        return ConfigSmsOtpRequiredAction.PROVIDER_ID.name();
    }
}
