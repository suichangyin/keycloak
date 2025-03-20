package org.keycloak.providers.phone.authentication.requiredactions;

import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.models.UserModel;

public class ConfigSmsOtpRequiredAction extends ConfigSmsWechatOtpRequiredAction {

    public static final UserModel.RequiredAction PROVIDER_ID = UserModel.RequiredAction.CONFIGURE_SMS_OTP;

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        Response challenge = context.form()
                .createForm("login-sms-otp-config.ftl");
        context.challenge(challenge);
    }
}
