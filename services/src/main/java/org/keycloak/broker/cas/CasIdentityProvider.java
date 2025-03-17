package org.keycloak.broker.cas;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.jboss.logging.Logger;
import org.keycloak.broker.cas.model.ServiceResponse;
import org.keycloak.broker.cas.model.Success;
import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;

import static org.keycloak.broker.cas.util.UrlHelper.PROVIDER_PARAMETER_STATE;
import static org.keycloak.broker.cas.util.UrlHelper.PROVIDER_PARAMETER_TICKET;
import static org.keycloak.broker.cas.util.UrlHelper.createAuthenticationUrl;
import static org.keycloak.broker.cas.util.UrlHelper.createLogoutUrl;
import static org.keycloak.broker.cas.util.UrlHelper.createValidateServiceUrl;

public class CasIdentityProvider extends AbstractIdentityProvider<CasIdentityProviderConfig> {

    protected static final Logger logger = Logger.getLogger(CasIdentityProvider.class);

    public static final String USER_ATTRIBUTES = "UserAttributes";

    private static final Unmarshaller unmarshaller;

    static {
        try {
            unmarshaller = JAXBContext.newInstance(ServiceResponse.class).createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public CasIdentityProvider(
            final KeycloakSession session, final CasIdentityProviderConfig config) {
        super(session, config);
    }

    @Override
    public Response performLogin(final AuthenticationRequest request) {
        try {
            URI authenticationUrl = createAuthenticationUrl(getConfig(), request).build();
            return Response.seeOther(authenticationUrl).build();
        } catch (Exception e) {
            throw new IdentityBrokerException(
                    "Could not send authentication request to cas provider.", e);
        }
    }

    @Override
    public Response keycloakInitiatedBrowserLogout(
            final KeycloakSession session,
            final UserSessionModel userSession,
            final UriInfo uriInfo,
            final RealmModel realm) {
        URI logoutUrl = createLogoutUrl(getConfig(), userSession, realm, uriInfo).build();
        return Response.status(302).location(logoutUrl).build();
    }

    @Override
    public Response retrieveToken(
            final KeycloakSession session, final FederatedIdentityModel identity) {
        return Response.ok(identity.getToken()).type(MediaType.APPLICATION_JSON).build();
    }

    @Override
    public Endpoint callback(
            final RealmModel realm,
            final AuthenticationCallback callback,
            final EventBuilder event) {
        return new Endpoint(callback, realm, event, this);
    }

    public static final class Endpoint {
        private final AuthenticationCallback callback;
        private final RealmModel realm;
        private final EventBuilder event;
        private final KeycloakSession session;
        private final ClientConnection clientConnection;
        private final HttpHeaders headers;
        private final CasIdentityProvider provider;
        private final CasIdentityProviderConfig config;

        Endpoint(
                final AuthenticationCallback callback,
                final RealmModel realm,
                final EventBuilder event,
                final CasIdentityProvider provider) {
            this.callback = callback;
            this.realm = realm;
            this.event = event;
            this.provider = provider;
            this.session = provider.session;
            this.headers = session.getContext().getRequestHeaders();
            this.clientConnection = session.getContext().getConnection();
            this.config = provider.getConfig();
        }

        @GET
        public Response authResponse(
                @QueryParam(PROVIDER_PARAMETER_TICKET) final String ticket,
                @QueryParam(PROVIDER_PARAMETER_STATE) final String state) {
            try {
                BrokeredIdentityContext federatedIdentity =
                        getFederatedIdentity(config, ticket, session.getContext().getUri(), state);

                return callback.authenticated(federatedIdentity);
            } catch (Exception e) {
                logger.error("Failed to complete CAS authentication", e);
            }
            event.event(EventType.LOGIN);
            event.error(Errors.IDENTITY_PROVIDER_LOGIN_FAILURE);
            return ErrorPage.error(
                    session, null, Status.EXPECTATION_FAILED, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
        }

        @GET
        @Path("logout_response")
        public Response logoutResponse(
                @Context final UriInfo uriInfo, @QueryParam("state") final String state) {
            UserSessionModel userSession = session.sessions().getUserSession(realm, state);
            if (userSession == null) {
                logger.error("no valid user session");
                EventBuilder e = new EventBuilder(realm, session, clientConnection);
                e.event(EventType.LOGOUT);
                e.error(Errors.USER_SESSION_NOT_FOUND);
                return ErrorPage.error(
                        session,
                        null,
                        Status.BAD_REQUEST,
                        Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
            }
            if (userSession.getState() != UserSessionModel.State.LOGGING_OUT) {
                logger.error("usersession in different state");
                EventBuilder e = new EventBuilder(realm, session, clientConnection);
                e.event(EventType.LOGOUT);
                e.error(Errors.USER_SESSION_NOT_FOUND);
                return ErrorPage.error(
                        session, null, Status.BAD_REQUEST, Messages.SESSION_NOT_ACTIVE);
            }
            return AuthenticationManager.finishBrowserLogout(
                    session, realm, userSession, uriInfo, clientConnection, headers);
        }

        private BrokeredIdentityContext getFederatedIdentity(
                final CasIdentityProviderConfig config,
                final String ticket,
                final UriInfo uriInfo,
                final String state) {
            logger.debug("Current state value: " + state);
            try (SimpleHttp.Response response =
                         SimpleHttp.doGet(
                                         createValidateServiceUrl(config, ticket, uriInfo, state)
                                                 .build()
                                                 .toURL()
                                                 .toString()
                                                 .replace("+", "%2B"),
                                         session)
                                 .asResponse()) {
                if (response.getStatus() != 200) {
                    logger.error(response.asString());
                    throw new IdentityBrokerException(
                            "CAS returned a non-200 response code: " + response.getStatus());
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("Raw XML from CAS: " + response.asString());
                }

                ServiceResponse serviceResponse =
                        (ServiceResponse) unmarshaller.unmarshal(new StringReader(response.asString()));

                if (logger.isDebugEnabled()) {
                    logger.debug("Parsed response: " + serviceResponse);
                }

                if (serviceResponse.getFailure() != null) {
                    throw new IdentityBrokerException(
                            "Failure response from CAS: "
                                    + serviceResponse.getFailure().getCode()
                                    + "("
                                    + serviceResponse.getFailure().getDescription()
                                    + ")");
                }
                Success success = serviceResponse.getSuccess();

                BrokeredIdentityContext user = new BrokeredIdentityContext(success.getUser(), config);
                user.setUsername(success.getUser());
                user.getContextData().put(USER_ATTRIBUTES, success.getAttributes());
                user.setIdp(provider);
                AuthenticationSessionModel authSession =
                        this.callback.getAndVerifyAuthenticationSession(state.replace(' ', '+'));
                session.getContext().setAuthenticationSession(authSession);
                user.setAuthenticationSession(authSession);
                return user;
            } catch (IOException | JAXBException e) {
                throw new IdentityBrokerException("Failed to complete CAS authentication", e);
            }
        }
    }
}
