package org.keycloak.providers.phone.providers.sender;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.providers.phone.providers.spi.MessageSenderService;
import org.keycloak.providers.phone.providers.spi.MessageSenderServiceProviderFactory;

public class WechatMessageSenderServiceProviderFactory implements MessageSenderServiceProviderFactory {

    private Config.Scope config;

    @Override
    public MessageSenderService create(KeycloakSession keycloakSession) {
        return new WechatSmsSenderServiceProvider(config, keycloakSession.getContext().getRealm());
    }

    @Override
    public void init(Config.Scope config) {
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
        return "wechat";
    }

}
