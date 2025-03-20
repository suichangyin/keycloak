package org.keycloak.providers.phone.providers.spi.impl;

import org.keycloak.providers.phone.providers.spi.PhoneProvider;
import org.keycloak.providers.phone.providers.spi.PhoneProviderFactory;
import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class PhoneMessageServiceProviderFactoryImpl implements PhoneProviderFactory {

    private Scope config;

    @Override
    public PhoneProvider create(KeycloakSession session) {
        return new DefaultPhoneProvider(session, config);
    }

    @Override
    public void init(Scope config) {
        this.config = config;
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "default";
    }
}
