package org.keycloak.providers.phone.authentication.requiredactions;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.credential.CredentialModel;
import org.keycloak.providers.phone.credential.PhoneOtpCredentialModel;
import org.keycloak.providers.phone.credential.PhoneOtpCredentialProvider;
import org.keycloak.providers.phone.credential.PhoneOtpCredentialProviderFactory;
import org.keycloak.providers.phone.providers.exception.PhoneNumberInvalidException;
import org.keycloak.providers.phone.providers.spi.PhoneVerificationCodeProvider;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.providers.phone.utils.UserUtils;
import org.keycloak.storage.UserStorageProvider;

import java.util.List;
import java.util.stream.Collectors;

public class ConfigSmsWechatOtpRequiredAction implements RequiredActionProvider {

    private static final Logger logger = Logger.getLogger(ConfigSmsWechatOtpRequiredAction.class);

    public static final String PROVIDER_ID = "CONFIGURE_SMS_WECHAT_OTP";

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        Response challenge = context.form()
                .createForm("login-sms-wechat-otp-config.ftl");
        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        PhoneVerificationCodeProvider codeProvider = context.getSession().getProvider(PhoneVerificationCodeProvider.class);
        String activeTab = context.getHttpRequest().getDecodedFormParameters().getFirst("activeTab");
        String countryCode = context.getHttpRequest().getDecodedFormParameters().getFirst("countryCode");
        String phoneNumber = context.getHttpRequest().getDecodedFormParameters().getFirst("phoneNumber");
        String openid = context.getHttpRequest().getDecodedFormParameters().getFirst("openid");
        String code = context.getHttpRequest().getDecodedFormParameters().getFirst("code");

        logger.warnf("processAction params: %s:%s:%s:%s:%s", activeTab, countryCode, phoneNumber, openid, code);

        if (activeTab == null || activeTab.isEmpty()) {
            activeTab = "phone";
        }

        if (activeTab.equals("wechat")) {
            if (openid != null && !openid.isEmpty()) {
                PhoneOtpCredentialProvider ocp = (PhoneOtpCredentialProvider) context.getSession()
                        .getProvider(CredentialProvider.class, PhoneOtpCredentialProviderFactory.PROVIDER_ID);

                removeUserCredentialByType(context, ocp, PhoneOtpCredentialModel.TYPE);

                ocp.createCredential(context.getRealm(), context.getUser(), PhoneOtpCredentialModel.create(openid, true));

                context.getUser().setSingleAttribute("phoneNumberVerified", "true");
                context.getUser().setSingleAttribute("wechat", "true");
                context.getUser().setSingleAttribute("phoneNumber", openid);

                context.success();
            } else {
                Response challenge = context.form()
                        .setError("noOngoingVerificationProcess")
                        .createForm("login-sms-wechat-otp-config.ftl");
                context.challenge(challenge);
            }
        } else if (activeTab.equals("phone")) {
            try {
                String internationalPhoneNumber = UserUtils.canonicalizePhoneNumber(context.getSession(), phoneNumber, countryCode);

                codeProvider.validateCode(context.getUser(), internationalPhoneNumber, code);
                PhoneOtpCredentialProvider ocp = (PhoneOtpCredentialProvider) context.getSession()
                        .getProvider(CredentialProvider.class, PhoneOtpCredentialProviderFactory.PROVIDER_ID);
                removeUserCredentialByType(context, ocp, PhoneOtpCredentialModel.TYPE);
                ocp.createCredential(context.getRealm(), context.getUser(), PhoneOtpCredentialModel.create(internationalPhoneNumber, false));

                context.getUser().setSingleAttribute("wechat", "false");

                context.success();
            } catch (BadRequestException e) {
                Response challenge = context.form()
                        .setError("noOngoingVerificationProcess")
                        .createForm("login-sms-wechat-otp-config.ftl");
                context.challenge(challenge);

            } catch (ForbiddenException e) {
                Response challenge = context.form()
                        .setAttribute("phoneNumber", phoneNumber)
                        .setError("verificationCodeDoesNotMatch")
                        .createForm("login-update-phone-number.ftl");
                context.challenge(challenge);

            } catch (PhoneNumberInvalidException e) {
                Response challenge = context.form()
                        .setError("noOngoingVerificationProcess")
                        .createForm("login-sms-wechat-otp-config.ftl");
                context.challenge(challenge);
            }
        } else {
            Response challenge = context.form()
                    .setError("noOngoingVerificationProcess")
                    .createForm("login-sms-wechat-otp-config.ftl");
            context.challenge(challenge);
        }
    }

    private void removeUserCredentialByType(RequiredActionContext context, PhoneOtpCredentialProvider ocp, String type) {
        List<CredentialModel> credentialModelList = context.getUser().credentialManager().getStoredCredentialsByTypeStream(type).collect(Collectors.toList());

        if (credentialModelList.size() > 0) {
            for (CredentialModel credentialModel : credentialModelList) {
                logger.debugf("deleteCredential: %s,%s,%s,%s",
                        credentialModel.getId(), credentialModel.getType(), credentialModel.getCredentialData(), credentialModel.getUserLabel());
                ocp.deleteCredential(context.getRealm(), context.getUser(), credentialModel.getId());
            }
        }
    }

    @Override
    public void close() {
    }
}
