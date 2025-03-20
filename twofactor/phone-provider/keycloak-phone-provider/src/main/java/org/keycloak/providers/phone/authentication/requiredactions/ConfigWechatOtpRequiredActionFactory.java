package org.keycloak.providers.phone.authentication.requiredactions;

import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;

public class ConfigWechatOtpRequiredActionFactory extends ConfigSmsWechatOtpRequiredActionFactory {

    private static final ConfigWechatOtpRequiredAction instance = new ConfigWechatOtpRequiredAction();

    @Override
    public String getDisplayText() {
        return "Configure OTP over Wechat";
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return instance;
    }

    @Override
    public String getId() {
        return ConfigWechatOtpRequiredAction.PROVIDER_ID;
    }
}
