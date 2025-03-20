package org.keycloak.providers.phone.providers.spi;

import org.keycloak.providers.phone.providers.constants.TokenCodeType;
import org.keycloak.providers.phone.providers.exception.MessageSendException;
import org.keycloak.models.KeycloakSession;

public abstract class FullSmsSenderAbstractService implements MessageSenderService{

    private final String realmDisplay;

    public FullSmsSenderAbstractService(String realmDisplay) {
        this.realmDisplay = realmDisplay;
    }

    public abstract void sendMessage(String phoneNumber, String message) throws MessageSendException;


    public void sendSmsMessage(TokenCodeType type, String phoneNumber, String code , int expires) throws MessageSendException{


        final String MESSAGE = String.format("[%s] - " + type.getLabel() + " code: %s, expires: %s minute ",realmDisplay , code, expires / 60);
        sendMessage(phoneNumber, MESSAGE);
    }
}
