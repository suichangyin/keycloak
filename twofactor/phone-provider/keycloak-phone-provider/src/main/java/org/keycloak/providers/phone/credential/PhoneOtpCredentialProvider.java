package org.keycloak.providers.phone.credential;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialTypeMetadata;
import org.keycloak.credential.CredentialTypeMetadataContext;
import org.keycloak.credential.UserCredentialStore;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.providers.phone.authentication.authenticators.browser.SmsOtpMfaAuthenticatorFactory;
import org.keycloak.providers.phone.authentication.authenticators.browser.SmsWechatOtpMfaAuthenticatorFactory;
import org.keycloak.providers.phone.authentication.authenticators.browser.WechatOtpMfaAuthenticatorFactory;
import org.keycloak.providers.phone.providers.constants.TokenCodeType;
import org.keycloak.providers.phone.providers.spi.PhoneVerificationCodeProvider;

public class PhoneOtpCredentialProvider implements CredentialProvider<PhoneOtpCredentialModel>, CredentialInputValidator {

    private final static Logger logger = Logger.getLogger(PhoneOtpCredentialProvider.class);
    private final KeycloakSession session;

    public PhoneOtpCredentialProvider(KeycloakSession session) {
        this.session = session;
    }

    private SubjectCredentialManager getCredentialStore(UserModel user) {
        return user.credentialManager();
    }

    private PhoneVerificationCodeProvider getTokenCodeService() {
        return session.getProvider(PhoneVerificationCodeProvider.class);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return getType().equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {

        String phoneNumber = user.getFirstAttribute("phoneNumber");
        String code = input.getChallengeResponse();

        if (!(input instanceof UserCredentialModel)) return false;
        if (!input.getType().equals(getType())) return false;
        if (phoneNumber == null) return false;
        if (code == null) return false;

        try {
            getTokenCodeService().validateCode(user, phoneNumber, code, TokenCodeType.OTP);
            return true;
        } catch (Exception e) {
            logger.warnf("Token code '%s' is not valid: %s", code, e.getMessage());
            return false;
        }
    }

    @Override
    public String getType() {
        return PhoneOtpCredentialModel.TYPE;
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, PhoneOtpCredentialModel credential) {
        if (credential.getCreatedDate() == null) {
            credential.setCreatedDate(Time.currentTimeMillis());
        }
        return getCredentialStore(user).createStoredCredential(credential);
    }

    @Override
    public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
        return getCredentialStore(user).removeStoredCredentialById(credentialId);
    }

    @Override
    public PhoneOtpCredentialModel getCredentialFromModel(CredentialModel credentialModel) {
        return PhoneOtpCredentialModel.createFromCredentialModel(credentialModel);
    }

    @Override
    public CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext credentialTypeMetadataContext) {
        return CredentialTypeMetadata.builder()
                .type(getType())
                .helpText("")
                .category(CredentialTypeMetadata.Category.TWO_FACTOR)
                .displayName(PhoneOtpCredentialProviderFactory.PROVIDER_ID)
                .createAction(SmsWechatOtpMfaAuthenticatorFactory.PROVIDER_ID)
                .createAction(SmsOtpMfaAuthenticatorFactory.PROVIDER_ID)
                .createAction(WechatOtpMfaAuthenticatorFactory.PROVIDER_ID)
                .removeable(true)
                .build(session);
    }
}
