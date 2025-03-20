package org.keycloak.providers.phone.providers.rest;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;
import org.keycloak.models.KeycloakSession;
import org.keycloak.providers.phone.providers.constants.TokenCodeType;
import org.keycloak.providers.phone.providers.exception.PhoneNumberInvalidException;
import org.keycloak.providers.phone.providers.spi.PhoneProvider;
import org.keycloak.providers.phone.utils.UserUtils;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;


public class TokenCodeResource {

    private static final Logger logger = Logger.getLogger(TokenCodeResource.class);
    protected final KeycloakSession session;
    protected final TokenCodeType tokenCodeType;

    TokenCodeResource(KeycloakSession session, TokenCodeType tokenCodeType) {
        this.session = session;
        this.tokenCodeType = tokenCodeType;
    }

    @GET
    @NoCache
    @Path("")
    @Produces(APPLICATION_JSON)
    public Response getTokenCode(@QueryParam("countryCode") String countryCode,
                                 @QueryParam("phoneNumber") String phoneNumber) {

        logger.warnf("TokenCodeResource param: %s:%s:%s", countryCode, phoneNumber);

        if (countryCode == null) countryCode = "CN";
        if (phoneNumber == null) throw new BadRequestException("Must inform a phone number");

        String internationalPhoneNumber;
        try {
            internationalPhoneNumber = UserUtils.canonicalizePhoneNumber(session, phoneNumber, countryCode);
        } catch (PhoneNumberInvalidException e) {
            throw new BadRequestException("Must inform a phone number");
        }

        logger.info(String.format("Requested %s code to %s",tokenCodeType.getLabel(), internationalPhoneNumber));
        int tokenExpiresIn = session.getProvider(PhoneProvider.class).sendTokenCode(internationalPhoneNumber,tokenCodeType, false);

        String response = String.format("{\"expiresIn\":%s}", tokenExpiresIn);

        return Response.ok(response, APPLICATION_JSON_TYPE).build();
    }
}
