package org.keycloak.providers.phone.authentication.authenticators.browser;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.CredentialValidator;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.providers.phone.authentication.requiredactions.ConfigWechatOtpRequiredAction;
import org.keycloak.providers.phone.credential.PhoneOtpCredentialProvider;
import org.keycloak.providers.phone.credential.PhoneOtpCredentialProviderFactory;
import org.keycloak.providers.phone.providers.constants.TokenCodeType;
import org.keycloak.providers.phone.providers.spi.PhoneProvider;

import java.net.URI;

public class WechatOtpMfaAuthenticator implements Authenticator, CredentialValidator<PhoneOtpCredentialProvider> {

    private static final Logger logger = Logger.getLogger(WechatOtpMfaAuthenticator.class);

    protected boolean hasCookie(AuthenticationFlowContext context) {
        Cookie cookie = context.getHttpRequest()
                .getHttpHeaders()
                .getCookies()
                .get("SMS_OTP_ANSWERED");
        return cookie != null;
    }

    protected boolean validateAnswer(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String secret = formData.getFirst("code");
        String credentialId = formData.getFirst("credentialId");
        if (credentialId == null || credentialId.isEmpty()) {
            credentialId = getCredentialProvider(context.getSession())
                    .getDefaultCredential(context.getSession(), context.getRealm(), context.getUser()).getId();
        }

        UserCredentialModel input = new UserCredentialModel(credentialId, getType(context.getSession()), secret);
        return getCredentialProvider(context.getSession()).isValid(context.getRealm(), context.getUser(), input);
    }

    protected void setCookie(AuthenticationFlowContext context) {

        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        int maxCookieAge = 60 * 60; // 1 hour

        if (config != null) maxCookieAge = Integer.parseInt(config.getConfig().get("cookie.max.age"));

        URI uri = context.getUriInfo()
                .getBaseUriBuilder()
                .path("realms")
                .path(context.getRealm().getName())
                .build();

        addCookie(context, "SMS_OTP_ANSWERED", "true",
                uri.getRawPath(),
                null, null,
                maxCookieAge,
                false, true);
    }

    public void addCookie(AuthenticationFlowContext context, String name, String value, String path, String domain, String comment, int maxAge, boolean secure, boolean httpOnly) {
//        HttpResponse response = context.getSession().getContext().getContextObject(HttpResponse.class);
//        StringBuffer cookieBuf = new StringBuffer();
        NewCookie cookie = new NewCookie.Builder(name)
                .value(value)
                .path(path)
                .domain(domain)
                .comment(comment)
                .maxAge(maxAge)
                .secure(secure)
                .httpOnly(httpOnly)
                .build();

        context.getSession().getContext().getHttpResponse().setCookieIfAbsent(cookie);
//        response.getOutputHeaders().add(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Override
    public PhoneOtpCredentialProvider getCredentialProvider(KeycloakSession session) {
        return (PhoneOtpCredentialProvider) session.getProvider(CredentialProvider.class, PhoneOtpCredentialProviderFactory.PROVIDER_ID);
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        if (hasCookie(context)) {
            context.success();
            return;
        }

        PhoneProvider phoneMessageService = context.getSession().getProvider(PhoneProvider.class);
        String phoneNumber = context.getUser().getFirstAttribute("phoneNumber");
        String wechat = context.getUser().getFirstAttribute("wechat");

        Response challenge;
        try {
            phoneMessageService.sendTokenCode(phoneNumber,
                    TokenCodeType.OTP, wechat != null && wechat.equalsIgnoreCase("true"));
            challenge = context.form().createForm("login-sms-otp.ftl");
        } catch (ForbiddenException e) {
            challenge = context.form().setError("abusedMessageService").createForm("login-sms-otp.ftl");
        }
        context.challenge(challenge);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        boolean validated = validateAnswer(context);
        if (!validated) {
            Response challenge = context.form()
                    .setError("authenticationCodeDoesNotMatch")
                    .createForm("login-sms-otp.ftl");
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
            return;
        }
        setCookie(context);
        context.success();
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return getCredentialProvider(session).isConfiguredFor(realm, user, getType(session));
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        user.addRequiredAction(ConfigWechatOtpRequiredAction.PROVIDER_ID);
    }

    @Override
    public void close() {

    }
}
