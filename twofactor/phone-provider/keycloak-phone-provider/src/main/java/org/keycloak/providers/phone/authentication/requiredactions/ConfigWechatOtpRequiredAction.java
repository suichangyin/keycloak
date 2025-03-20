package org.keycloak.providers.phone.authentication.requiredactions;

import org.keycloak.authentication.RequiredActionContext;

import jakarta.ws.rs.core.Response;

public class ConfigWechatOtpRequiredAction extends ConfigSmsWechatOtpRequiredAction {

    public static final String PROVIDER_ID = "CONFIGURE_WECHAT_OTP";

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        Response challenge = context.form()
                .createForm("login-wechat-otp-config.ftl");
        context.challenge(challenge);
    }
}
