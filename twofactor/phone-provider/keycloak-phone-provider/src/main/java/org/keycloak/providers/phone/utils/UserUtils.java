package org.keycloak.providers.phone.utils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import jakarta.validation.constraints.NotNull;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.providers.phone.providers.exception.PhoneNumberInvalidException;
import org.keycloak.providers.phone.providers.spi.PhoneProvider;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserUtils {

    private static final Logger logger = Logger.getLogger(UserUtils.class);

    private static UserModel singleUser(List<UserModel> users) {
        if (users.isEmpty()) {
            return null;
        } else if (users.size() > 1) {
            return users.stream()
                    .filter(u -> u.getAttributeStream("phoneNumberVerified").anyMatch("true"::equals))
                    .findFirst().orElse(null);
        } else
            return users.get(0);
    }

    public static UserModel findUserByPhone(UserProvider userProvider, RealmModel realm, String phoneNumber) {
        List<UserModel> users = userProvider.searchForUserByUserAttributeStream(
                realm, "phoneNumber", phoneNumber).collect(Collectors.toList());
        return singleUser(users);
    }

    public static UserModel findUserByPhone(UserProvider userProvider, RealmModel realm, String phoneNumber, String notIs) {
        List<UserModel> users = userProvider.searchForUserByUserAttributeStream(
                realm, "phoneNumber", phoneNumber).collect(Collectors.toList());
        return singleUser(users.stream().filter(u -> !u.getId().equals(notIs)).collect(Collectors.toList()));
    }

    public static String canonicalizePhoneNumber(KeycloakSession session, @NotNull String phoneNumber, @Nullable String region) throws PhoneNumberInvalidException {
        PhoneProvider provider = session.getProvider(PhoneProvider.class);

        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        String resultPhoneNumber = phoneNumber.trim();
        if (region != null && !region.isEmpty()) {
            if (!phoneNumberUtil.getSupportedRegions().contains(region)) {
                String defaultRegion = defaultRegion(session);
                logger.warnf("Default region '%s' will be used, does not support the region '%s'.", defaultRegion, region);
                region = defaultRegion;
            }
        }

        try {
            Phonenumber.PhoneNumber parsedNumber = phoneNumberUtil.parse(resultPhoneNumber, region);
            if (provider.validPhoneNumber() && !phoneNumberUtil.isValidNumber(parsedNumber)) {
                logger.info(String.format("Phone number [%s] Valid fail with google's libphonenumber", resultPhoneNumber));
                throw new PhoneNumberInvalidException(PhoneNumberInvalidException.ErrorType.VALID_FAIL,
                        String.format("Phone number [%s] Valid fail with google's libphonenumber", resultPhoneNumber));
            }

            Optional<String> canonicalizeFormat = provider.canonicalizePhoneNumber();
            try {
                resultPhoneNumber = canonicalizeFormat
                        .map(PhoneNumberUtil.PhoneNumberFormat::valueOf)
                        .map(format -> phoneNumberUtil.format(parsedNumber, format))
                        .orElse(resultPhoneNumber);
            } catch (RuntimeException e) {
                logger.warn(String.format("canonicalize format param error! '%s' is not in supported list: %s, E164 Will be used.",
                        Arrays.toString(PhoneNumberUtil.PhoneNumberFormat.values()),
                        canonicalizeFormat.orElse("")), e);
                resultPhoneNumber = phoneNumberUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
            }

            Optional<String> phoneNumberRegex = provider.phoneNumberRegex();
            if (!phoneNumberRegex.map(resultPhoneNumber::matches).orElse(true)) {
                logger.info(String.format("Phone number [%s] not match regex '%s'", resultPhoneNumber, phoneNumberRegex.orElse("")));
                throw new PhoneNumberInvalidException(PhoneNumberInvalidException.ErrorType.NOT_SUPPORTED,
                        String.format("Phone number [%s] not match regex '%s'", resultPhoneNumber, phoneNumberRegex.orElse("")));
            }

            return phoneNumberUtil.format(phoneNumberUtil.parse(resultPhoneNumber, region), PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            logger.info(e);
            throw new PhoneNumberInvalidException(e);
        }
    }

    private static String defaultRegion(KeycloakSession session) {
        Optional<String> defaultRegion = session.getProvider(PhoneProvider.class).defaultPhoneRegion();
        return defaultRegion.orElseGet(() -> localeToCountry(session.getContext().getRealm().getDefaultLocale()).orElse(null));
    }

    private static Optional<String> localeToCountry(String locale) {
        return OptionalUtils.ofBlank(locale).flatMap(l -> {
            Pattern countryRegx = Pattern.compile("[^a-z]*\\-?([A-Z]{2,3})");
            return Optional.of(countryRegx.matcher(l))
                    .flatMap(m -> m.find() ? OptionalUtils.ofBlank(m.group(1)) : Optional.empty());
        });
    }

    public static boolean isDuplicatePhoneAllowed() {
        //TODO isDuplicatePhoneAllowed
        return true;
    }
}
