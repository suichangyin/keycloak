package org.keycloak.providers.email.authentication.authenticators.browser;

import org.apache.commons.validator.routines.EmailValidator;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.providers.email.authentication.requiredactions.ConfigEmailOtpRequiredAction;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EmailAuthenticatorForm extends AbstractUsernameFormAuthenticator {
    private static final Logger logger = Logger.getLogger(EmailAuthenticatorForm.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        challenge(context, null);
    }

    @Override
    protected Response challenge(AuthenticationFlowContext context, String error) {
        UserModel user = context.getUser();

        Response response;
        LoginFormsProvider form = context.form().setExecution(context.getExecution().getId());
        if (error != null) {
            form.setError(error);
        }

        if (user.getEmail() == null || user.getEmail().isEmpty() || !user.isEmailVerified()) {
            response = context.form().createForm("login-email-otp-config.ftl");
        } else {
            generateAndSendEmailCode(context);
            response = form.createForm("email-code-form.ftl");
        }

        context.challenge(response);
        return response;
    }

    private void generateAndSendEmailCode(AuthenticationFlowContext context) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        AuthenticationSessionModel session = context.getAuthenticationSession();

        if (session.getAuthNote(EmailConstants.CODE) != null) {
            // skip sending email code
            return;
        }

        int length = EmailConstants.DEFAULT_LENGTH;
        int ttl = EmailConstants.DEFAULT_TTL;
        if (config != null) {
            // get config values
            length = Integer.parseInt(config.getConfig().get(EmailConstants.CODE_LENGTH));
            ttl = Integer.parseInt(config.getConfig().get(EmailConstants.CODE_TTL));
        }

        String code = generateTokenCode();
        // for test
        if (length != 6) {
            logger.infof("Simulate sending the verification code: %s to %s", code, context.getUser().getEmail());
        } else {
            logger.debugf("Send the verification code: %s to %s", code, context.getUser().getEmail());
            sendEmailWithCode(context.getSession(), context.getRealm(), context.getUser(), code, ttl);
        }
        session.setAuthNote(EmailConstants.CODE, code);
        session.setAuthNote(EmailConstants.CODE_TTL, Long.toString(System.currentTimeMillis() + (ttl * 1000L)));
    }

    private static String generateTokenCode() {
        SecureRandom secureRandom = new SecureRandom();
        Integer code = secureRandom.nextInt(999_999);
        return String.format("%06d", code);
    }

    protected Response challengeConfig(AuthenticationFlowContext context, String error) {
        Response response;
        LoginFormsProvider form = context.form().setExecution(context.getExecution().getId());
        if (error != null) {
            form.setError(error);
        }

        response = context.form().createForm("login-email-otp-config.ftl");

        context.challenge(response);
        return response;
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        UserModel userModel = context.getUser();
        if (!enabledUser(context, userModel)) {
            // error in context is set in enabledUser/isDisabledByBruteForce
            return;
        }

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (formData.containsKey("sendConfig") && formData.containsKey(EmailConstants.EMAIL_ADDRESS)) {
            String emailAddress = formData.getFirst(EmailConstants.EMAIL_ADDRESS);
            boolean validEmail = EmailValidator.getInstance().isValid(emailAddress);
            if (validEmail) {
                // check email is been used by another user
                List<UserModel> userModels = context.getSession().users()
                        .searchForUserStream(context.getRealm(), Collections.singletonMap(UserModel.EMAIL, emailAddress))
                        .filter(u -> !u.getUsername().equals(userModel.getUsername()))
                        .collect(Collectors.toList());
                if (userModels.isEmpty()) {
                    userModel.setEmail(emailAddress);
                    resetEmailCode(context);
                    generateAndSendEmailCode(context);
                    context.getAuthenticationSession().setAuthNote("email", emailAddress);
                    challengeConfig(context, null);
                } else {
                    challengeConfig(context, Errors.EMAIL_IN_USE);
                }
            } else {
                challengeConfig(context, Errors.INVALID_EMAIL);
            }
            return;
        }

        if (formData.containsKey("resend")) {
            resetEmailCode(context);
            challenge(context, null);
            return;
        }

        if (formData.containsKey("cancel")) {
            resetEmailCode(context);
            context.resetFlow();
            return;
        }

        AuthenticationSessionModel session = context.getAuthenticationSession();
        String code = session.getAuthNote(EmailConstants.CODE);
        String ttl = session.getAuthNote(EmailConstants.CODE_TTL);
        String enteredCode = formData.getFirst(EmailConstants.CODE);

        if (enteredCode != null && enteredCode.equals(code)) {
            if (Long.parseLong(ttl) < System.currentTimeMillis()) {
                // expired
                context.getEvent().user(userModel).error(Errors.EXPIRED_CODE);
                Response challengeResponse = challenge(context, Messages.EXPIRED_ACTION_TOKEN_SESSION_EXISTS);
                context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE, challengeResponse);
            } else {
                // valid
                resetEmailCode(context);
                userModel.setEmailVerified(true);
                context.success();
            }
        } else {
            // invalid
            AuthenticationExecutionModel execution = context.getExecution();
            if (execution.isRequired()) {
                context.getEvent().user(userModel).error(Errors.INVALID_USER_CREDENTIALS);
                Response challengeResponse = challenge(context, Messages.INVALID_ACCESS_CODE);
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
            } else if (execution.isConditional() || execution.isAlternative()) {
                context.attempted();
            }
        }
    }

    private void resetEmailCode(AuthenticationFlowContext context) {
        context.getAuthenticationSession().removeAuthNote(EmailConstants.CODE);
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        user.addRequiredAction(ConfigEmailOtpRequiredAction.PROVIDER_ID);
    }

    @Override
    public void close() {
        // NOOP
    }

    private void sendEmailWithCode(KeycloakSession session, RealmModel realm, UserModel user, String code, int ttl) {
        if (user.getEmail() == null) {
            logger.warnf("Could not send access code email due to missing email. realm=%s user=%s", realm.getId(), user.getUsername());
            throw new AuthenticationFlowException(AuthenticationFlowError.INVALID_USER);
        }

        Map<String, Object> mailBodyAttributes = new HashMap<>();
        mailBodyAttributes.put("username", user.getUsername());
        mailBodyAttributes.put("code", code);
        mailBodyAttributes.put("ttl", ttl);

        String realmName = realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName();
        List<Object> subjectParams = Arrays.asList(realmName);
        try {
            EmailTemplateProvider emailProvider = session.getProvider(EmailTemplateProvider.class);
            emailProvider.setRealm(realm);
            emailProvider.setUser(user);
            // Don't forget to add the welcome-email.ftl (html and text) template to your theme.
            emailProvider.send("emailCodeSubject", subjectParams, "code-email.ftl", mailBodyAttributes);
        } catch (EmailException eex) {
            logger.errorf(eex, "Failed to send access code email. realm=%s user=%s", realm.getId(), user.getUsername());
        }
    }
}
