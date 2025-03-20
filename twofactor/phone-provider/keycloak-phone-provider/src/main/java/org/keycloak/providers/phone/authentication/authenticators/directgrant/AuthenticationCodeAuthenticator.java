package org.keycloak.providers.phone.authentication.authenticators.directgrant;

import org.keycloak.providers.phone.providers.constants.TokenCodeType;
import org.keycloak.providers.phone.providers.spi.PhoneVerificationCodeProvider;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;


public class AuthenticationCodeAuthenticator extends BaseDirectGrantAuthenticator {

    private static final Logger logger = Logger.getLogger(AuthenticationCodeAuthenticator.class);

    public AuthenticationCodeAuthenticator(KeycloakSession session) {
        if (session.getContext().getRealm() == null) {
            throw new IllegalStateException("The service cannot accept a session without a realm in its context.");
        }
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        user.addRequiredAction("VERIFICATION_CODE_GRANT_CONFIG");
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        if (!validateVerificationCode(context,getPhoneNumber(context))) {
            invalidCredentials(context,context.getUser());
            return;
        }
        context.success();
    }



    protected boolean validateVerificationCode(AuthenticationFlowContext context, String phoneNumber) {

        String code = context.getHttpRequest().getDecodedFormParameters().getFirst("code");

//        String kind = null;
//        AuthenticatorConfigModel authenticatorConfig = context.getAuthenticatorConfig();
//        if (authenticatorConfig != null && authenticatorConfig.getConfig() != null) {
//            kind = Optional.ofNullable(context.getAuthenticatorConfig().getConfig().get(AuthenticationCodeAuthenticatorFactory.KIND)).orElse("");
//        }

        try {
            context.getSession().getProvider(PhoneVerificationCodeProvider.class).validateCode(context.getUser(), phoneNumber, code, TokenCodeType.OTP);
            return true;
        } catch (Exception e) {
            logger.info("Grant authenticator valid code failure");
            return false;
        }

    }

}
