package org.keycloak.providers.phone.providers.spi.impl;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.ServiceUnavailableException;
import org.keycloak.providers.phone.providers.spi.PhoneVerificationCodeProvider;
import org.keycloak.providers.phone.providers.constants.TokenCodeType;
import org.keycloak.providers.phone.providers.exception.MessageSendException;
import org.keycloak.providers.phone.providers.representations.TokenCodeRepresentation;
import org.keycloak.providers.phone.providers.spi.MessageSenderService;
import org.keycloak.providers.phone.providers.spi.PhoneProvider;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.providers.phone.utils.OptionalUtils;

import java.time.Instant;
import java.util.Optional;

public class DefaultPhoneProvider implements PhoneProvider {

    private static final Logger logger = Logger.getLogger(DefaultPhoneProvider.class);
    private final KeycloakSession session;
    private final Scope config;
    private final String service;
    private final int tokenExpiresIn;

    DefaultPhoneProvider(KeycloakSession session, Scope config) {
        this.session = session;

        this.config = config;

        this.service = session.listProviderIds(MessageSenderService.class)
                .stream().filter(s -> s.equalsIgnoreCase(config.get("service")))
                .findFirst().orElse("dummy");

        this.tokenExpiresIn = config.getInt("tokenExpiresIn", 60);
    }

    @Override
    public void close() {
    }


    private PhoneVerificationCodeProvider getTokenCodeService() {
        return session.getProvider(PhoneVerificationCodeProvider.class);
    }

    private String getRealmName() {
        return session.getContext().getRealm().getName();
    }

    private Optional<String> getStringConfigValue(String configName) {
        return OptionalUtils.ofBlank(OptionalUtils.ofBlank(config.get(getRealmName() + "-" + configName))
                .orElse(config.get(configName)));
    }

    private boolean getBooleanConfigValue(String configName, boolean defaultValue) {
        Boolean result = config.getBoolean(getRealmName() + "-" + configName, null);
        if (result == null) {
            result = config.getBoolean(configName, defaultValue);
        }
        return result;
    }

    @Override
    public boolean isDuplicatePhoneAllowed() {
        return getBooleanConfigValue("duplicate-phone", false);
    }

    @Override
    public boolean validPhoneNumber() {
        return getBooleanConfigValue("valid-phone", true);
    }

    @Override
    public boolean compatibleMode() {
        return getBooleanConfigValue("compatible", false);
    }

    @Override
    public int otpExpires() {
        return getStringConfigValue("otp-expires").map(Integer::valueOf).orElse(60 * 60);
    }

    @Override
    public Optional<String> canonicalizePhoneNumber() {
        return getStringConfigValue("canonicalize-phone-numbers");
    }

    @Override
    public Optional<String> defaultPhoneRegion() {
        return getStringConfigValue("phone-default-region");
    }

    @Override
    public Optional<String> phoneNumberRegex() {
        return getStringConfigValue("number-regex");
    }


    @Override
    public int sendTokenCode(String phoneNumber, TokenCodeType type, boolean wechat) {
        String sendService = wechat ? "wechat" : service;

        if (!wechat && getTokenCodeService().isAbusing(phoneNumber, type)) {
            throw new ForbiddenException("You requested the maximum number of messages the last hour");
        }

        TokenCodeRepresentation ongoing = getTokenCodeService().ongoingProcess(phoneNumber, type);
        if (ongoing != null) {
            logger.info(String.format("No need of sending a new %s code for %s", type.getLabel(), phoneNumber));
            return (int) (ongoing.getExpiresAt().getTime() - Instant.now().toEpochMilli()) / 1000;
        }

        TokenCodeRepresentation token = TokenCodeRepresentation.forPhoneNumber(phoneNumber);

        try {
            session.getProvider(MessageSenderService.class, sendService)
                    .sendSmsMessage(type, phoneNumber, token.getCode(), tokenExpiresIn);

            getTokenCodeService().persistCode(token, type, tokenExpiresIn);

            logger.info(String.format("Sent %s code to %s over %s", type.getLabel(), phoneNumber, sendService));

        } catch (MessageSendException e) {

            logger.error(String.format("Message sending to %s failed by %s with %s: %s",
                    phoneNumber, sendService, e.getErrorCode(), e.getErrorMessage()));
            throw new ServiceUnavailableException("Send sms failed. Please retry...");
        }

        return tokenExpiresIn;
    }
}
