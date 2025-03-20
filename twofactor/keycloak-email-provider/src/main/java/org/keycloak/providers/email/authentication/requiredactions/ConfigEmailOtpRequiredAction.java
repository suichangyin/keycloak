package org.keycloak.providers.email.authentication.requiredactions;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.UserModel;


public class ConfigEmailOtpRequiredAction implements RequiredActionProvider {

    private static final Logger logger = Logger.getLogger(ConfigEmailOtpRequiredAction.class);

    public static final UserModel.RequiredAction PROVIDER_ID = UserModel.RequiredAction.CONFIGURE_EMAIL_OTP;

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        Response challenge = context.form()
                .createForm("login-sms-otp-config.ftl");
        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        String activeTab = context.getHttpRequest().getDecodedFormParameters().getFirst("activeTab");
        String countryCode = context.getHttpRequest().getDecodedFormParameters().getFirst("countryCode");
        String phoneNumber = context.getHttpRequest().getDecodedFormParameters().getFirst("phoneNumber");
        String openid = context.getHttpRequest().getDecodedFormParameters().getFirst("openid");
        String code = context.getHttpRequest().getDecodedFormParameters().getFirst("code");

        logger.warnf("processAction params: %s:%s:%s:%s:%s", activeTab, countryCode, phoneNumber, openid, code);

        if (activeTab.equals("wechat")) {
            if (openid != null && !openid.isEmpty()) {
//                PhoneOtpCredentialProvider ocp = (PhoneOtpCredentialProvider) context.getSession()
//                        .getProvider(CredentialProvider.class, PhoneOtpCredentialProviderFactory.PROVIDER_ID);
//
//                removeUserCredentialByType(context, ocp, PhoneOtpCredentialModel.TYPE);
//
//                ocp.createCredential(context.getRealm(), context.getUser(), PhoneOtpCredentialModel.create(openid, true));

                context.getUser().setSingleAttribute("email", phoneNumber);

                context.success();
            } else {
                Response challenge = context.form()
                        .setError("noOngoingVerificationProcess")
                        .createForm("login-sms-otp-config.ftl");
                context.challenge(challenge);
            }
        } else if (activeTab.equals("phone")) {
            try {
//                String internationalPhoneNumber = UserUtils.canonicalizePhoneNumber(context.getSession(), phoneNumber, countryCode);
//
//                codeProvider.validateCode(context.getUser(), internationalPhoneNumber, code);
//                PhoneOtpCredentialProvider ocp = (PhoneOtpCredentialProvider) context.getSession()
//                        .getProvider(CredentialProvider.class, PhoneOtpCredentialProviderFactory.PROVIDER_ID);
//                removeUserCredentialByType(context, ocp, PhoneOtpCredentialModel.TYPE);
//                ocp.createCredential(context.getRealm(), context.getUser(), PhoneOtpCredentialModel.create(internationalPhoneNumber, false));

                context.getUser().setSingleAttribute("wechat", "false");

                context.success();
            } catch (BadRequestException e) {
                Response challenge = context.form()
                        .setError("noOngoingVerificationProcess")
                        .createForm("login-sms-otp-config.ftl");
                context.challenge(challenge);

            } catch (ForbiddenException e) {
                Response challenge = context.form()
                        .setAttribute("phoneNumber", phoneNumber)
                        .setError("verificationCodeDoesNotMatch")
                        .createForm("login-update-phone-number.ftl");
                context.challenge(challenge);

//            } catch (PhoneNumberInvalidException e) {
//                Response challenge = context.form()
//                        .setError("noOngoingVerificationProcess")
//                        .createForm("login-sms-wechat-otp-config.ftl");
//                context.challenge(challenge);
            }
        } else {
            Response challenge = context.form()
                    .setError("noOngoingVerificationProcess")
                    .createForm("login-sms-otp-config.ftl");
            context.challenge(challenge);
        }
    }

    private void removeUserCredentialByType(RequiredActionContext context, String type) {
        context.getUser().credentialManager().getStoredCredentialsByTypeStream(type)
                .forEach(credentialModel -> {
                            logger.errorf("deleteCredential: %s,%s,%s,%s",
                                    credentialModel.getId(), credentialModel.getType(), credentialModel.getCredentialData(), credentialModel.getUserLabel());
//                    ocp.deleteCredential(context.getRealm(), context.getUser(), credentialModel.getId());
                        }
                );

    }

    @Override
    public void close() {
    }
}
