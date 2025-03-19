package org.keycloak.federation.rest.api.user.object.service;


import org.keycloak.federation.rest.api.user.object.response.UserResponseObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * Stub Service class to be used with RestEasy to access user rest api
 * Header	Description
 * X-Total-Pages	The total number of pages
 * X-Per-Page	The number of items per page
 * X-Page	The index of the current page (starting at 1)
 */
@Produces(MediaType.APPLICATION_JSON)
public interface UserServiceOdataObject{

    @GET
    @Path("")
    UserResponseObject getUsers(@QueryParam("$skip") int skip,
                                @QueryParam("$top") int top,
                                @QueryParam("$count") boolean count);

    @GET
    @Path("")
    UserResponseObject getUsers(@QueryParam("$skip") int skip,
                                @QueryParam("$top") int top,
                                @QueryParam("$count") boolean count,
                                @QueryParam("$filter") String filter,
                                @QueryParam("$order") String order);

    @GET
    @Path("")
    UserResponseObject getUpdatedUsers(@QueryParam("$skip") int skip,
                                       @QueryParam("$top") int top,
                                       @QueryParam("$count") boolean count);

    @GET
    @Path("")
    UserResponseObject getUpdatedUsers(@QueryParam("$skip") int skip,
                                       @QueryParam("$top") int top,
                                       @QueryParam("$count") boolean count,
                                       @QueryParam("$filter") String filter,
                                       @QueryParam("$order") String order);
}
