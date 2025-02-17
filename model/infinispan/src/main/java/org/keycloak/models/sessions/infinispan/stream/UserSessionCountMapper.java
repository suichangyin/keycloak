package org.keycloak.models.sessions.infinispan.stream;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Function;

@ProtoTypeId(Marshalling.USER_SESSION_MAPPER)
public class UserSessionCountMapper implements Function<Map.Entry<String, SessionEntityWrapper<UserSessionEntity>>, AbstractMap.SimpleEntry<String, Long>>, Serializable {

    private static final UserSessionCountMapper INSTANCE = new UserSessionCountMapper();

    private UserSessionCountMapper() {
    }

    @ProtoFactory
    public static UserSessionCountMapper getInstance() {
        return INSTANCE;
    }

    @Override
    public AbstractMap.SimpleEntry<String, Long> apply(Map.Entry<String, SessionEntityWrapper<UserSessionEntity>> entry) {
        UserSessionEntity entity = entry.getValue().getEntity();
        return new AbstractMap.SimpleEntry<>(entity.getUser(), Long.valueOf(entity.getAuthenticatedClientSessions().size()));
    }
}
