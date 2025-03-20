package org.keycloak.providers.phone.authentication.authenticators.directgrant;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.providers.phone.utils.UserUtils;
import org.keycloak.services.validation.Validation;

public class PhoneNumberAuthenticator extends BaseDirectGrantAuthenticator {

    private static final Logger logger = Logger.getLogger(PhoneNumberAuthenticator.class);

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        user.addRequiredAction("PHONE_NUMBER_GRANT_CONFIG");
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        String phoneNumber = getPhoneNumber(context);

        if (Validation.isBlank(phoneNumber)){
            invalidCredentials(context);
            return;
        }
        UserModel user = UserUtils.findUserByPhone(context.getSession().users(),context.getRealm(),phoneNumber);
        if (user == null) {
            invalidCredentials(context);
            return;
        }

        logger.info("Grant authenticator valid phone success");
        context.setUser(user);
        context.success();
    }
}
