package org.keycloak.providers.email.authentication.requiredactions;

import org.keycloak.Config.Scope;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class ConfigEmailOtpRequiredActionFactory implements RequiredActionFactory {

    private static final ConfigEmailOtpRequiredAction instance = new ConfigEmailOtpRequiredAction();

    @Override
    public String getDisplayText() {
        return "Configure OTP over Email";
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return instance;
    }

    @Override
    public void init(Scope scope) {
    }

    @Override
    public void postInit(KeycloakSessionFactory sessionFactory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ConfigEmailOtpRequiredAction.PROVIDER_ID.name();
    }
}
