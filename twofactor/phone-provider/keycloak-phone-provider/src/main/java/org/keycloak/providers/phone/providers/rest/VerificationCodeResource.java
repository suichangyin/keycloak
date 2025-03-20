package org.keycloak.providers.phone.providers.rest;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.providers.phone.providers.constants.TokenCodeType;
import org.keycloak.providers.phone.providers.exception.PhoneNumberInvalidException;
import org.keycloak.providers.phone.providers.spi.PhoneVerificationCodeProvider;
import org.keycloak.providers.phone.utils.UserUtils;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager.AuthResult;

import java.util.Map;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;


public class VerificationCodeResource extends TokenCodeResource {

    private static final Logger logger = Logger.getLogger(VerificationCodeResource.class);

    private final AuthResult auth;

    VerificationCodeResource(KeycloakSession session) {
        super(session, TokenCodeType.VERIFY);
        this.auth = new AppAuthManager().authenticateIdentityCookie(session, session.getContext().getRealm());
    }

    private PhoneVerificationCodeProvider getTokenCodeService() {
        return session.getProvider(PhoneVerificationCodeProvider.class);
    }

    @POST
    @NoCache
    @Path("")
    @Produces(APPLICATION_JSON)
    public Response checkVerificationCode(@QueryParam("countryCode") String countryCode,
                                          @QueryParam("phoneNumber") String phoneNumber,
                                          @QueryParam("code") String code) {

        if (auth == null) throw new NotAuthorizedException("Bearer");
        if (countryCode == null) countryCode = "CN";
        if (phoneNumber == null) throw new BadRequestException("Must inform a phone number");
        if (code == null) throw new BadRequestException("Must inform a token code");

        String internationalPhoneNumber;
        try {
            internationalPhoneNumber = UserUtils.canonicalizePhoneNumber(session, phoneNumber, countryCode);
        } catch (PhoneNumberInvalidException e) {
            throw new BadRequestException("Must inform a phone number");
        }

        logger.warnf("checkVerificationCode: %s:%s", internationalPhoneNumber, code);

        UserModel user = auth.getUser();
        getTokenCodeService().validateCode(user, internationalPhoneNumber, code);

        return Response.noContent().build();
    }
}
