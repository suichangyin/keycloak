package org.keycloak.providers.phone.authentication.requiredactions;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import org.keycloak.providers.phone.providers.spi.PhoneVerificationCodeProvider;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;


public class UpdatePhoneNumberRequiredAction implements RequiredActionProvider {

    public static final String PROVIDER_ID = "UPDATE_PHONE_NUMBER";

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        Response challenge = context.form()
                .createForm("login-update-phone-number.ftl");
        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        PhoneVerificationCodeProvider tokenCodeService = context.getSession().getProvider(PhoneVerificationCodeProvider.class);
        String phoneNumber = context.getHttpRequest().getDecodedFormParameters().getFirst("phoneNumber");
        String code = context.getHttpRequest().getDecodedFormParameters().getFirst("code");
        try {
            tokenCodeService.validateCode(context.getUser(), phoneNumber, code);
            context.success();
        } catch (BadRequestException e) {

            Response challenge = context.form()
                    .setError("noOngoingVerificationProcess")
                    .createForm("login-update-phone-number.ftl");
            context.challenge(challenge);

        } catch (ForbiddenException e) {

            Response challenge = context.form()
                    .setAttribute("phoneNumber", phoneNumber)
                    .setError("verificationCodeDoesNotMatch")
                    .createForm("login-update-phone-number.ftl");
            context.challenge(challenge);
        }
    }

    @Override
    public void close() {
    }
}
