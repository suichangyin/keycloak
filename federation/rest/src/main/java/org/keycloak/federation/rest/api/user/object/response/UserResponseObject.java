package org.keycloak.federation.rest.api.user.object.response;

import org.jboss.resteasy.annotations.Body;
import org.jboss.resteasy.annotations.ResponseObject;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;

import jakarta.ws.rs.HeaderParam;

@ResponseObject
public interface UserResponseObject {

    @HeaderParam("X-Page")
    String page();
    @HeaderParam("X-Total-Pages")
    String totalPages();
    @HeaderParam("X-Per-Page")
    String perPage();
    @Body
    String body();

    ClientResponse response();
}
