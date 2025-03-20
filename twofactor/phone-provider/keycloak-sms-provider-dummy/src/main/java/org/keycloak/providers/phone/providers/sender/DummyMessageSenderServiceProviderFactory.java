package org.keycloak.providers.phone.providers.sender;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.providers.phone.providers.spi.MessageSenderService;
import org.keycloak.providers.phone.providers.spi.MessageSenderServiceProviderFactory;

public class DummyMessageSenderServiceProviderFactory implements MessageSenderServiceProviderFactory {

    @Override
    public MessageSenderService create(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        return new DummySmsSenderService(realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName());
    }

    @Override
    public void init(Config.Scope scope) {
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "dummy";
    }
}
