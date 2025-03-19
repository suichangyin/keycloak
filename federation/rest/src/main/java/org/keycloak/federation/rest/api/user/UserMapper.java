package org.keycloak.federation.rest.api.user;

import org.keycloak.federation.rest.model.RestUser;

import java.util.Set;

/**
 * Methods for users synchronization
 */
public interface UserMapper {

    /**
     * Full users
     *
     * @return Users
     */
    Set<RestUser> getUsers();

    /**
     * Updated users
     *
     * @return Users
     */
    Set<RestUser> getUpdatedUsers(String date);
}
