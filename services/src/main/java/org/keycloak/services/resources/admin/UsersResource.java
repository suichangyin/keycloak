/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.services.resources.admin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.github.stuxuhai.jpinyin.PinyinException;
import com.github.stuxuhai.jpinyin.PinyinFormat;
import com.github.stuxuhai.jpinyin.PinyinHelper;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.InternalServerErrorException;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelIllegalStateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.light.LightweightUserAdapter;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.policy.PasswordPolicyNotMetException;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.MgmtPermissions;
import org.keycloak.services.resources.admin.permissions.UserPermissionEvaluator;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.utils.SearchQueryUtils;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.models.utils.KeycloakModelUtils.findGroupByPath;
import static org.keycloak.userprofile.UserProfileContext.USER_API;

/**
 * Base resource for managing users
 *
 * @resource Users
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class UsersResource {

    private static final Logger logger = Logger.getLogger(UsersResource.class);
    private static final String SEARCH_ID_PARAMETER = "id:";

    protected final RealmModel realm;

    private final AdminPermissionEvaluator auth;

    private final AdminEventBuilder adminEvent;

    protected final ClientConnection clientConnection;

    protected final KeycloakSession session;

    protected final HttpHeaders headers;

    public UsersResource(KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.clientConnection = session.getContext().getConnection();
        this.auth = auth;
        this.realm = session.getContext().getRealm();
        this.adminEvent = adminEvent.resource(ResourceType.USER);
        this.headers = session.getContext().getRequestHeaders();
    }

    /**
     * Create a new user
     *
     * Username must be unique.
     *
     * @param rep
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.USERS)
    @Operation( summary = "Create a new user Username must be unique.")
    public Response createUser(final UserRepresentation rep) {
        // first check if user has manage rights
        try {
            auth.users().requireManage();
        } catch (ForbiddenException exception) {
            if (!canCreateGroupMembers(rep)) {
                throw exception;
            }
        }

        if (rep.getId() != null) {
            if (session.users().getUserById(realm, rep.getId()) != null) {
                throw ErrorResponse.error("User with ID already exists", Response.Status.CONFLICT);
            }

            try {
                if (!rep.getId().equals(UUID.fromString(rep.getId()).toString())) {
                    throw new IllegalArgumentException("");
                }
            } catch (IllegalArgumentException exception) {
                throw ErrorResponse.error("Invalid user ID", Response.Status.BAD_REQUEST);
            }
        }

        if (rep.getUid() != null) {
            if (rep.getUid() <= 0) {
                throw ErrorResponse.error("Invalid user UID", Response.Status.BAD_REQUEST);
            }
        }

        String username = rep.getUsername();
        if(realm.isRegistrationEmailAsUsername()) {
            username = rep.getEmail();
        }

        UserProfileProvider profileProvider = session.getProvider(UserProfileProvider.class);

        UserProfile profile = profileProvider.create(USER_API, rep.getRawAttributes());

        try {
            Response response = UserResource.validateUserProfile(profile, session, auth.adminAuth());
            if (response != null) {
                return response;
            }

            List<CredentialRepresentation> credentials = rep.getCredentials();
            if (credentials == null || credentials.size() == 0) {
                CredentialRepresentation cred = new CredentialRepresentation();
                cred.setType(CredentialRepresentation.PASSWORD);
                cred.setValue(Constants.DEFAULT_PASSWORD);
                cred.setTemporary(true);
                cred.setIgnorePasswordPolicy(true);
                rep.setCredentials(Collections.singletonList(cred));
            }

            UserModel user = profile.create();

            UserResource.updateUserFromRep(profile, user, rep, session, false);
            RepresentationToModel.createFederatedIdentities(rep, session, realm, user);
            RepresentationToModel.createGroups(session, rep, realm, user);

            RepresentationToModel.createCredentials(rep, session, realm, user, true);
            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), user.getId()).representation(rep).success();

            return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(user.getId()).build()).build();
        } catch (ModelDuplicateException e) {
            throw ErrorResponse.exists("User exists with same username or email");
        } catch (PasswordPolicyNotMetException e) {
            logger.warn("Password policy not met for user " + e.getUsername(), e);
            Properties messages = AdminRoot.getMessages(session, realm, auth.adminAuth().getToken().getLocale());
            throw new ErrorResponseException(e.getMessage(), MessageFormat.format(messages.getProperty(e.getMessage(), e.getMessage()), e.getParameters()),
                    Response.Status.BAD_REQUEST);
        } catch (ModelIllegalStateException e) {
            logger.error(e.getMessage(), e);
            throw ErrorResponse.error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        } catch (ModelException me){
            logger.warn("Could not create user", me);
            throw ErrorResponse.error("Could not create user", Response.Status.BAD_REQUEST);
        }
    }

    private boolean canCreateGroupMembers(UserRepresentation rep) {
        if (!Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ)) {
            return false;
        }

        List<GroupModel> groups = Optional.ofNullable(rep.getGroups())
                .orElse(Collections.emptyList())
                .stream().map(path -> findGroupByPath(session, realm, path))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (groups.isEmpty()) {
            return false;
        }

        // if groups is part of the user rep, check if admin has manage_members and manage_membership on each group
        // an exception is thrown in case the current user does not have permissions to manage any of the groups
        for (GroupModel group : groups) {
            auth.groups().requireManageMembers(group);
            auth.groups().requireManageMembership(group);
        }

        return true;
    }

    /**
     * Get representation of the user
     *
     * @param id User id
     * @return
     */
    @Path("{user-id}")
    public UserResource user(final @PathParam("user-id") String id) {
        UserModel user = null;
        if (LightweightUserAdapter.isLightweightUser(id)) {
            UserSessionModel userSession = session.sessions().getUserSession(realm, LightweightUserAdapter.getLightweightUserId(id));
            if (userSession != null) {
                user = userSession.getUser();
            }
        } else {
            user = session.users().getUserById(realm, id);
        }

        if (user == null) {
            // we do this to make sure somebody can't phish ids
            if (auth.users().canQuery()) throw new NotFoundException("User not found");
            else throw new ForbiddenException();
        }

        return new UserResource(session, user, auth, adminEvent);
    }

    /**
     * Get users
     *
     * Returns a stream of users, filtered according to query parameters.
     *
     * @param search A String contained in username, first or last name, or email. Default search behavior is prefix-based (e.g., <code>foo</code> or <code>foo*</code>). Use <code>*foo*</code> for infix search and <code>"foo"</code> for exact search.
     * @param last A String contained in lastName, or the complete lastName, if param "exact" is true
     * @param first A String contained in firstName, or the complete firstName, if param "exact" is true
     * @param email A String contained in email, or the complete email, if param "exact" is true
     * @param username A String contained in username, or the complete username, if param "exact" is true
     * @param emailVerified whether the email has been verified
     * @param idpAlias The alias of an Identity Provider linked to the user
     * @param idpUserId The userId at an Identity Provider linked to the user
     * @param firstResult Pagination offset
     * @param maxResults Maximum results size (defaults to 100)
     * @param enabled Boolean representing if user is enabled or not
     * @param briefRepresentation Boolean which defines whether brief representations are returned (default: false)
     * @param exact Boolean which defines whether the params "last", "first", "email" and "username" must match exactly
     * @param searchQuery A query to search for custom attributes, in the format 'key1:value2 key2:value2'
     * @return a non-null {@code Stream} of users
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.USERS)
    @Operation( summary = "Get users Returns a stream of users, filtered according to query parameters.")
    public Stream<UserRepresentation> getUsers(
            @Parameter(description = "A String contained in username, first or last name, or email. Default search behavior is prefix-based (e.g., foo or foo*). Use *foo* for infix search and \"foo\" for exact search.") @QueryParam("search") String search,
            @Parameter(description = "A String contained in lastName, or the complete lastName, if param \"exact\" is true") @QueryParam("lastName") String last,
            @Parameter(description = "A String contained in firstName, or the complete firstName, if param \"exact\" is true") @QueryParam("firstName") String first,
            @Parameter(description = "A String contained in email, or the complete email, if param \"exact\" is true") @QueryParam("email") String email,
            @Parameter(description = "A String contained in username, or the complete username, if param \"exact\" is true") @QueryParam("username") String username,
            @Parameter(description = "whether the email has been verified") @QueryParam("emailVerified") Boolean emailVerified,
            @Parameter(description = "The alias of an Identity Provider linked to the user") @QueryParam("idpAlias") String idpAlias,
            @Parameter(description = "The userId at an Identity Provider linked to the user") @QueryParam("idpUserId") String idpUserId,
            @Parameter(description = "Pagination offset") @QueryParam("first") Integer firstResult,
            @Parameter(description = "Maximum results size (defaults to 100)") @QueryParam("max") Integer maxResults,
            @Parameter(description = "Boolean representing if user is enabled or not") @QueryParam("enabled") Boolean enabled,
            @Parameter(description = "Boolean which defines whether brief representations are returned (default: false)") @QueryParam("briefRepresentation") Boolean briefRepresentation,
            @Parameter(description = "Boolean which defines whether the params \"last\", \"first\", \"email\" and \"username\" must match exactly") @QueryParam("exact") Boolean exact,
            @Parameter(description = "A query to search for custom attributes, in the format 'key1:value2 key2:value2'") @QueryParam("q") String searchQuery) {
        UserPermissionEvaluator userPermissionEvaluator = auth.users();

        userPermissionEvaluator.requireQuery();

        firstResult = firstResult != null ? firstResult : -1;
        maxResults = maxResults != null ? maxResults : Constants.DEFAULT_MAX_RESULTS;

        Map<String, String> searchAttributes = searchQuery == null
                ? Collections.emptyMap()
                : SearchQueryUtils.getFields(searchQuery);

        Stream<UserModel> userModels = Stream.empty();
        if (search != null) {
            if (search.startsWith(SEARCH_ID_PARAMETER)) {
                UserModel userModel =
                        session.users().getUserById(realm, search.substring(SEARCH_ID_PARAMETER.length()).trim());
                if (userModel != null) {
                    userModels = Stream.of(userModel);
                }
            } else {
                Map<String, String> attributes = new HashMap<>();
                attributes.put(UserModel.SEARCH, search.trim());
                if (enabled != null) {
                    attributes.put(UserModel.ENABLED, enabled.toString());
                }
                return searchForUser(attributes, realm, userPermissionEvaluator, briefRepresentation, firstResult,
                        maxResults, false);
            }
        } else if (last != null || first != null || email != null || username != null || emailVerified != null
                || idpAlias != null || idpUserId != null || enabled != null || exact != null || !searchAttributes.isEmpty()) {
                    Map<String, String> attributes = new HashMap<>();
                    if (last != null) {
                        attributes.put(UserModel.LAST_NAME, last);
                    }
                    if (first != null) {
                        attributes.put(UserModel.FIRST_NAME, first);
                    }
                    if (email != null) {
                        attributes.put(UserModel.EMAIL, email);
                    }
                    if (username != null) {
                        attributes.put(UserModel.USERNAME, username);
                    }
                    if (emailVerified != null) {
                        attributes.put(UserModel.EMAIL_VERIFIED, emailVerified.toString());
                    }
                    if (idpAlias != null) {
                        attributes.put(UserModel.IDP_ALIAS, idpAlias);
                    }
                    if (idpUserId != null) {
                        attributes.put(UserModel.IDP_USER_ID, idpUserId);
                    }
                    if (enabled != null) {
                        attributes.put(UserModel.ENABLED, enabled.toString());
                    }
                    if (exact != null) {
                        attributes.put(UserModel.EXACT, exact.toString());
                    }

                    attributes.putAll(searchAttributes);

                    return searchForUser(attributes, realm, userPermissionEvaluator, briefRepresentation, firstResult,
                            maxResults, true);
                } else {
                    return searchForUser(new HashMap<>(), realm, userPermissionEvaluator, briefRepresentation,
                            firstResult, maxResults, false);
                }

        return toRepresentation(realm, userPermissionEvaluator, briefRepresentation, userModels);
    }

    /**
     * Returns the number of users that match the given criteria.
     * It can be called in three different ways.
     * 1. Don't specify any criteria and pass {@code null}. The number of all
     * users within that realm will be returned.
     * <p>
     * 2. If {@code search} is specified other criteria such as {@code last} will
     * be ignored even though you set them. The {@code search} string will be
     * matched against the first and last name, the username and the email of a
     * user.
     * <p>
     * 3. If {@code search} is unspecified but any of {@code last}, {@code first},
     * {@code email} or {@code username} those criteria are matched against their
     * respective fields on a user entity. Combined with a logical and.
     *
     * @param search   arbitrary search string for all the fields below. Default search behavior is prefix-based (e.g., <code>foo</code> or <code>foo*</code>). Use <code>*foo*</code> for infix search and <code>"foo"</code> for exact search.
     * @param last     last name filter
     * @param first    first name filter
     * @param email    email filter
     * @param username username filter
     * @param enabled Boolean representing if user is enabled or not
     * @return the number of users that match the given criteria
     */
    @Path("count")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.USERS)
    @Operation(
            summary = "Returns the number of users that match the given criteria.",
            description = "It can be called in three different ways. " +
                    "1. Don’t specify any criteria and pass {@code null}. The number of all users within that realm will be returned. <p> " +
                    "2. If {@code search} is specified other criteria such as {@code last} will be ignored even though you set them. The {@code search} string will be matched against the first and last name, the username and the email of a user. <p> " +
                    "3. If {@code search} is unspecified but any of {@code last}, {@code first}, {@code email} or {@code username} those criteria are matched against their respective fields on a user entity. Combined with a logical and.")
    public Integer getUsersCount(
            @Parameter(description = "arbitrary search string for all the fields below. Default search behavior is prefix-based (e.g., foo or foo*). Use *foo* for infix search and \"foo\" for exact search.") @QueryParam("search") String search,
            @Parameter(description = "last name filter") @QueryParam("lastName") String last,
            @Parameter(description = "first name filter") @QueryParam("firstName") String first,
            @Parameter(description = "email filter") @QueryParam("email") String email,
            @QueryParam("emailVerified") Boolean emailVerified,
            @Parameter(description = "username filter") @QueryParam("username") String username,
            @Parameter(description = "Boolean representing if user is enabled or not") @QueryParam("enabled") Boolean enabled,
            @QueryParam("q") String searchQuery) {
        UserPermissionEvaluator userPermissionEvaluator = auth.users();
        userPermissionEvaluator.requireQuery();

        Map<String, String> searchAttributes = searchQuery == null
                ? Collections.emptyMap()
                : SearchQueryUtils.getFields(searchQuery);

        if (search != null) {
            if (search.startsWith(SEARCH_ID_PARAMETER)) {
                UserModel userModel = session.users().getUserById(realm, search.substring(SEARCH_ID_PARAMETER.length()).trim());
                return userModel != null && userPermissionEvaluator.canView(userModel) ? 1 : 0;
            } else if (userPermissionEvaluator.canView()) {
                return session.users().getUsersCount(realm, search.trim());
            } else {
                return session.users().getUsersCount(realm, search.trim(), auth.groups().getGroupsWithViewPermission());
            }
        } else if (last != null || first != null || email != null || username != null || emailVerified != null || enabled != null || !searchAttributes.isEmpty()) {
            Map<String, String> parameters = new HashMap<>();
            if (last != null) {
                parameters.put(UserModel.LAST_NAME, last);
            }
            if (first != null) {
                parameters.put(UserModel.FIRST_NAME, first);
            }
            if (email != null) {
                parameters.put(UserModel.EMAIL, email);
            }
            if (username != null) {
                parameters.put(UserModel.USERNAME, username);
            }
            if (emailVerified != null) {
                parameters.put(UserModel.EMAIL_VERIFIED, emailVerified.toString());
            }
            if (enabled != null) {
                parameters.put(UserModel.ENABLED, enabled.toString());
            }
            parameters.putAll(searchAttributes);

            if (userPermissionEvaluator.canView()) {
                return session.users().getUsersCount(realm, parameters);
            } else {
                return session.users().getUsersCount(realm, parameters, auth.groups().getGroupsWithViewPermission());
            }
        } else if (userPermissionEvaluator.canView()) {
            return session.users().getUsersCount(realm);
        } else {
            return session.users().getUsersCount(realm, auth.groups().getGroupsWithViewPermission());
        }
    }

    /**
     * Get representation of the user
     *
     * @return
     */
    @Path("profile")
    public UserProfileResource userProfile() {
        return new UserProfileResource(session, auth, adminEvent);
    }

    private Stream<UserRepresentation> searchForUser(Map<String, String> attributes, RealmModel realm, UserPermissionEvaluator usersEvaluator, Boolean briefRepresentation, Integer firstResult, Integer maxResults, Boolean includeServiceAccounts) {
        attributes.put(UserModel.INCLUDE_SERVICE_ACCOUNT, includeServiceAccounts.toString());

        if (!auth.users().canView()) {
            Set<String> groupModels = auth.groups().getGroupsWithViewPermission();

            if (!groupModels.isEmpty()) {
                session.setAttribute(UserModel.GROUPS, groupModels);
            }
        }

        Stream<UserModel> userModels = session.users().searchForUserStream(realm, attributes, firstResult, maxResults);
        return toRepresentation(realm, usersEvaluator, briefRepresentation, userModels);
    }

    private Stream<UserRepresentation> toRepresentation(RealmModel realm, UserPermissionEvaluator usersEvaluator, Boolean briefRepresentation, Stream<UserModel> userModels) {
        boolean briefRepresentationB = briefRepresentation != null && briefRepresentation;
        boolean canViewGlobal = usersEvaluator.canView();

        usersEvaluator.grantIfNoPermission(session.getAttribute(UserModel.GROUPS) != null);
        return userModels.filter(user -> canViewGlobal || usersEvaluator.canView(user))
                .map(user -> {
                    UserRepresentation userRep = briefRepresentationB
                            ? ModelToRepresentation.toBriefRepresentation(user)
                            : ModelToRepresentation.toRepresentation(session, realm, user);
                    userRep.setAccess(usersEvaluator.getAccess(user));
                    return userRep;
                });
    }

    public static class User {
        @JsonUnwrapped
        private UserRepresentation rep;
        private String createdTime;

        public User() {
        }

        public User(UserRepresentation rep) {
            this.rep = rep;

            if (rep.getCreatedTimestamp() != null) {
                this.createdTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
                        .withZone(ZoneId.systemDefault())
                        .format(Instant.ofEpochMilli(rep.getCreatedTimestamp()));
            }
        }

        @JsonIgnore
        public UserRepresentation getUser() {
            return rep;
        }

        public String getCreatedTime() {
            return createdTime;
        }
    }

    public static String getName(UserModel user) {
        String name = "";
        if (ObjectUtil.isBlank(user.getLastName()) && ObjectUtil.isBlank(user.getFirstName())) {
            name = user.getUsername();
        } else {
            if (!ObjectUtil.isBlank(user.getLastName())) {
                name = user.getLastName();
            }
            if (!ObjectUtil.isBlank(user.getFirstName())) {
                name += user.getFirstName();
            }
        }

        return name;
    }

    public static boolean searchUserByName(UserModel user, CharSequence search) {
        if (user.getUsername().contains(search)) {
            return true;
        }

        String name = "";
        if (!ObjectUtil.isBlank(user.getLastName())) {
            name += user.getLastName();
        }
        if (!ObjectUtil.isBlank(user.getFirstName())) {
            name += user.getFirstName();
        }

        return name.contains(search);
    }

    public static class IndexComparator implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {
            if (s1.equals(s2)) {
                return 0;
            }
            if (s1.equals("#")) {
                return 1;
            }
            if (s2.equals("#")) {
                return -1;
            }
            return s1.compareTo(s2);
        }
    }

    @Path("all")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<User>> getAllUsers(@QueryParam("search") String search,
                                               @QueryParam("ungrouped") Boolean ungrouped,
                                               @QueryParam("enabled") Boolean enabled,
                                               @QueryParam("briefRepresentation") Boolean briefRepresentation) {
        auth.users().requireQuery();

        List<Predicate<UserModel>> predicates = new ArrayList<>();
        if (ungrouped != null && ungrouped) {
            predicates.add(u -> u.getGroupsCount() == 0);
        }
        if (enabled != null) {
            predicates.add(u -> u.isEnabled() == enabled);
        }
        if (!ObjectUtil.isBlank(search)) {
            CharSequence seq = search.trim();
            predicates.add(((Predicate<UserModel>) u -> searchUserByName(u, seq))
                    .or(u -> u.getEmail() != null && u.getEmail().contains(seq))
                    .or(u -> u.getId().contains(seq)));
        }

        Predicate<UserModel> filter = predicates.stream().reduce(u -> true, Predicate::and);
        Supplier<Stream<UserModel>> sup = () -> session.users().searchForUserStream(realm, Collections.emptyMap()).filter(filter);

        Map<String, String> idNameMap = new HashMap<>();
        sup.get().forEach(u -> {
            idNameMap.put(u.getId(), getName(u));
        });

        Map<String, String> pinyin = new HashMap<>();
        for (Map.Entry<String, String> entry : idNameMap.entrySet()) {
            try {
                pinyin.put(entry.getKey(), PinyinHelper.convertToPinyinString(entry.getValue(), "", PinyinFormat.WITH_TONE_NUMBER));
            } catch (PinyinException e) {
                e.printStackTrace();
                return Collections.<String, List<User>>emptyMap();
            }
        }

        Comparator<UserModel> sortByPinyin = Comparator.comparing(u -> Collections.unmodifiableMap(pinyin).get(u.getId()));
        Comparator<UserModel> sortByName = Comparator.comparing(u -> getName(u));
        Comparator<UserModel> sortByUserName = Comparator.comparing(u -> u.getUsername());

        boolean briefRep = briefRepresentation != null && briefRepresentation;
        Map<String, List<User>> results = new TreeMap<>(new IndexComparator());
        sup.get().sorted(sortByPinyin.thenComparing(sortByName).thenComparing(sortByUserName))
                .forEach(u -> {
                    UserRepresentation rep = briefRep ? ModelToRepresentation.toBriefRepresentation(u) : ModelToRepresentation.toRepresentation(session, realm, u);
                    if (session.getProvider(BruteForceProtector.class).isTemporarilyDisabled(session, realm, u)) {
                        rep.setEnabled(false);
                    }
                    char c = pinyin.get(u.getId()).charAt(0);
                    if (!Character.isLetter(c)) {
                        c = '#';
                    }
                    String s = Character.toString(Character.toUpperCase(c));
                    List<User> users = results.get(s);
                    if (users == null) {
                        users = new LinkedList<>();
                    }
                    users.add(new User(rep));
                    results.put(s, users);
                });

        return results;
    }

    @Path("all/count")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Long> getAllUsersCount(@QueryParam("search") String search,
                                              @QueryParam("enabled") Boolean enabled,
                                              @QueryParam("ungrouped") Boolean ungrouped) {
        auth.users().requireQuery();

        List<Predicate<UserModel>> predicates = new ArrayList<>();
        if (ungrouped != null && ungrouped) {
            predicates.add(u -> u.getGroupsCount() == 0);
        }
        if (enabled != null) {
            predicates.add(u -> u.isEnabled() == enabled);
        }
        if (!ObjectUtil.isBlank(search)) {
            CharSequence seq = search.trim();
            predicates.add(((Predicate<UserModel>) u -> searchUserByName(u, seq))
                    .or(u -> u.getEmail() != null && u.getEmail().contains(seq))
                    .or(u -> u.getId().contains(seq)));
        }

        Predicate<UserModel> filter = predicates.stream().reduce(u -> true, Predicate::and);
        return Collections.singletonMap("count", session.users().searchForUserStream(realm, Collections.emptyMap()).filter(filter).count());
    }

    @Path("all2")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<User> getAll2Users(@QueryParam("search") String search,
                                   @QueryParam("ungrouped") Boolean ungrouped,
                                   @QueryParam("first") Integer firstResult,
                                   @QueryParam("max") Integer maxResults,
                                   @QueryParam("briefRepresentation") Boolean briefRepresentation) {
        UserPermissionEvaluator userPermissionEvaluator = auth.users();

        userPermissionEvaluator.requireQuery();

        List<User> results = new LinkedList<>();

        Integer finalFirstResult = firstResult != null ? firstResult : -1;
        Integer finalMaxResults = maxResults != null ? maxResults : Constants.DEFAULT_MAX_RESULTS;

        Supplier<Stream<UserModel>> sup;
        try {
            if (ungrouped != null && ungrouped) {
                Integer finalFirstResult1 = finalFirstResult == -1 ? 0 : finalFirstResult;
                sup = () -> session.users().getUsersNoGroupStream(realm)
                        .filter(u -> search == null ||
                                u.getUsername().toLowerCase().contains(search.trim().toLowerCase()) ||
                                (u.getFirstName() != null && u.getFirstName().toLowerCase().contains(search.trim().toLowerCase())) ||
                                (u.getLastName() != null && u.getLastName().toLowerCase().contains(search.trim().toLowerCase())) ||
                                (u.getFirstName() != null && u.getLastName() != null && (u.getLastName() + u.getFirstName()).toLowerCase().contains(search.trim().toLowerCase())) ||
                                (u.getEmail() != null && u.getEmail().toLowerCase().contains(search.trim().toLowerCase())))
                        .sorted(Comparator.comparing(UserModel::getUsername))
                        .skip(finalFirstResult1)
                        .limit(finalMaxResults);
            } else {
                if (search != null) {
                    sup = () -> session.users().searchForUserStream(realm,
                            Map.of(UserModel.SEARCH, search, UserModel.INCLUDE_SERVICE_ACCOUNT, Boolean.FALSE.toString()),
                            finalFirstResult,
                            finalMaxResults);
                } else {
                    sup = () -> session.users().searchForUserStream(realm,
                            Map.of(UserModel.INCLUDE_SERVICE_ACCOUNT, Boolean.FALSE.toString()),
                            finalFirstResult,
                            finalMaxResults);
                }
            }


            boolean briefRep = briefRepresentation != null && briefRepresentation;

            sup.get().forEach(u -> {
                UserRepresentation rep = briefRep ? ModelToRepresentation.toBriefRepresentation(u) : ModelToRepresentation.toRepresentation(session, realm, u);
                if (session.getProvider(BruteForceProtector.class).isTemporarilyDisabled(session, realm, u)) {
                    rep.setEnabled(false);
                }
                results.add(new User(rep));
            });

//        } catch (LdapAuthenticationException e) {
//            throw new InternalServerErrorException("Error when trying to connect to LDAP, please check the ldap configuration.");
        } catch (Exception e) {
            throw new InternalServerErrorException("Error when trying to connect to LDAP, please check the ldap configuration.");
        }

        return results;
    }

    @Path("all2/count")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Integer> getAll2UsersCount(@QueryParam("search") String search,
                                                  @QueryParam("ungrouped") @DefaultValue("false") Boolean ungrouped) {
        auth.users().requireQuery();

        int result;

        try {
            if (ungrouped) {
                result = (int) session.users().getUsersNoGroupStream(realm)
                        .filter(u -> search == null || search.trim().isEmpty() ||
                                u.getUsername().toLowerCase().contains(search.trim().toLowerCase()) ||
                                (u.getFirstName() != null && u.getFirstName().toLowerCase().contains(search.trim().toLowerCase())) ||
                                (u.getLastName() != null && u.getLastName().toLowerCase().contains(search.trim().toLowerCase())) ||
                                (u.getFirstName() != null && u.getLastName() != null && (u.getLastName() + u.getFirstName()).toLowerCase().contains(search.trim().toLowerCase())) ||
                                (u.getEmail() != null && u.getEmail().toLowerCase().contains(search.trim().toLowerCase())))
                        .count();
            } else {
                if (search == null || search.trim().isEmpty()) {
                    result = session.users().getUsersCount(realm);
                } else {
                    result = session.users().getUsersCount(realm, Map.of(UserModel.SEARCH, search, UserModel.INCLUDE_SERVICE_ACCOUNT, Boolean.FALSE.toString()));
                }
            }
//        } catch (LdapAuthenticationException e) {
//            throw new InternalServerErrorException("Error when trying to connect to LDAP, please check the ldap configuration.");
        } catch (Exception e) {
            throw new InternalServerErrorException("Error when trying to connect to LDAP, please check the ldap configuration.");
        }

        return Collections.singletonMap("count", result);
    }

    /**
     * Delete users
     *
     * @param ids
     * @return
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteUsers(final List<String> ids) {
        auth.users().requireManage();

        for (String id : ids) {
            UserModel user = session.users().getUserById(realm, id);
            if (user == null) {
                // we do this to make sure somebody can't phish ids
                if (auth.users().canQuery()) {
                    throw new NotFoundException(String.format("User with ID %s not found", id));
                } else {
                    throw new ForbiddenException();
                }
            }

            if (auth instanceof MgmtPermissions && ((MgmtPermissions) auth).admin().equals(user)) {
                throw ErrorResponse.error("You are not allowed to delete your account.", Response.Status.BAD_REQUEST);
            }

            Set<GroupModel> groups = user.getGroupsStream().collect(Collectors.toSet());
            for (GroupModel group : groups) {
                try {
                    user.leaveGroup(group);
                } catch (ModelException e) {
                    if (e.getMessage().equals("Not possible to delete LDAP group mappings as mapper mode is READ_ONLY")) {
                        break;
                    }

                    throw ErrorResponse.error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
                }
            }

            boolean removed = new UserManager(session).removeUser(realm, user);
            if (!removed) {
                throw ErrorResponse.error(String.format("User with ID %s couldn't be deleted", id), Response.Status.BAD_REQUEST);
            }

            adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri(), id).success();
        }

        return Response.noContent().build();
    }
}
