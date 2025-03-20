package org.keycloak.providers.phone.providers.spi;

import org.keycloak.provider.Provider;
import org.keycloak.providers.phone.providers.constants.TokenCodeType;
import org.keycloak.providers.phone.providers.exception.MessageSendException;

/**
 * SMS, Voice, APP
 */
public interface MessageSenderService extends Provider {

    //void sendVoiceMessage((TokenCodeType type, String realmName, String realmDisplayName, String phoneNumber, String code , int expires) throws MessageSendException;


    void sendSmsMessage(TokenCodeType type, String phoneNumber, String code , int expires) throws MessageSendException;
}
