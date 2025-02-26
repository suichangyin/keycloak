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

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.github.stuxuhai.jpinyin.PinyinException;
import com.github.stuxuhai.jpinyin.PinyinFormat;
import com.github.stuxuhai.jpinyin.PinyinHelper;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.NoCache;
import org.keycloak.common.Profile;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.GroupModel.GroupPathChangeEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.provider.Provider;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.ManagementPermissionReference;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissionManagement;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.utils.GroupUtils;
import org.keycloak.utils.ProfileHelper;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.utils.StreamsUtil.paginatedStream;

/**
 * @resource Groups
 * @author Bill Burke
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class GroupResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private final GroupModel group;

    public GroupResource(RealmModel realm, GroupModel group, KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.GROUP);
        this.group = group;
    }

    public static class Group {
        @JsonUnwrapped
        private GroupRepresentation rep;
        private long totalMembers;

        public Group(GroupRepresentation rep) {
            this.rep = rep;
        }

        public Group(GroupRepresentation rep, int totalMembers) {
            this.rep = rep;
            this.totalMembers = totalMembers;
        }

        public Group(KeycloakSession session, RealmModel realm, GroupModel group) {
            this(session, realm, group, true);
        }

        public Group(KeycloakSession session, RealmModel realm, GroupModel group, boolean briefRepresentation) {
            this.rep = ModelToRepresentation.toGroupHierarchy(group, !briefRepresentation);
//            this.totalMembers = getGroupMemberIds(session, realm, group).size();
            this.totalMembers = getGroupAllMembersCount(session, realm, group, false, null);
        }

        public void setAccess(Map<String, Boolean> access) {
            this.rep.setAccess(access);
        }

        public long getTotalMembers() {
            return totalMembers;
        }
    }

     /**
     *
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.GROUPS)
    @Operation()
    public GroupRepresentation getGroup() {
        this.auth.groups().requireView(group);

        GroupRepresentation rep = GroupUtils.toRepresentation(this.auth.groups(), group, true);

        rep.setAccess(auth.groups().getAccess(group));

        return GroupUtils.populateSubGroupCount(group, rep);
    }

    /**
     * Update group, ignores subgroups.
     *
     * @param rep
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.GROUPS)
    @Operation( summary = "Update group, ignores subgroups.")
    public Response updateGroup(GroupRepresentation rep) {
        this.auth.groups().requireManage(group);

        String groupName = rep.getName();

        if (ObjectUtil.isBlank(groupName)) {
            if (rep.getDisplayName() != null) {
                groupName = group.getName();
            } else {
                throw ErrorResponse.error("Group name is missing", Response.Status.BAD_REQUEST);
            }
        }

        if (rep.getId() != null && !group.getId().equals(rep.getId())) {
            throw ErrorResponse.error("Invalid group id", Response.Status.BAD_REQUEST);
        }

        if (rep.getGid() != null && rep.getGid() <= 0) {
            throw ErrorResponse.error("Invalid group GID", Response.Status.BAD_REQUEST);
        }

        if (!Objects.equals(groupName, group.getName())) {
            String finalGroupName = groupName;
            boolean exists = siblings().filter(s -> !Objects.equals(s.getId(), group.getId()))
                    .anyMatch(s -> Objects.equals(s.getName(), finalGroupName));
            if (exists) {
                throw ErrorResponse.exists("Sibling group named '" + groupName + "' already exists.");
            }
        }

        updateGroup(rep, group, realm, session);
        adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).representation(rep).success();
        
        return Response.noContent().build();
    }
    
    private Stream<GroupModel> siblings() {
        if (group.getParentId() == null) {
            return session.groups().getTopLevelGroupsStream(realm);
        } else {
            return group.getParent().getSubGroupsStream();
        }
    }

    private Set<UserModel> getGroupAndSubGroupMembers(GroupModel group) {
        Set<UserModel> users = new HashSet<>();

        for (GroupModel child : group.getSubGroupsStream().collect(Collectors.toList())) {
            users.addAll(getGroupAndSubGroupMembers(child));
        }

        users.addAll(session.users().getGroupMembersStream(realm, group).collect(Collectors.toList()));

        return users;
    }

    @DELETE
    @Tag(name = KeycloakOpenAPI.Admin.Tags.GROUPS)
    @Operation()
    public void deleteGroup(@QueryParam("deleteMembers") Boolean deleteMembers) {
        this.auth.groups().requireManage(group);

        Set<UserModel> users = new HashSet<>();
        if (deleteMembers != null && deleteMembers) {
            this.auth.users().requireManage();
            users = getGroupAndSubGroupMembers(group);
        }

//        LDAPStorageMapper ldapGroupMapper = getLDAPGroupMapper(session, realm);
//
//        if (ldapGroupMapper != null) {
//            ldapGroupMapper.removeGroup(realm, group);
//        }

        realm.removeGroup(group);

        for (UserModel userModel:users) {
            session.users().removeUser(realm, userModel);
        }

        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
    }

    @DELETE
    @NoCache
    @Path("target/none")
    public void deleteGroupMoveMembers() {
        this.auth.groups().requireManage(group);

        List<String> members = session.users()
                .getGroupMembersStream(realm, group)
                .map(u -> u.getUsername())
                .collect(Collectors.toList());
        if (members.size() != 0) {
            throw new BadRequestException(String.format("Moving members %s to realm is not allowed.", String.join(", ", members)));
        }

//        LDAPStorageMapper ldapGroupMapper = getLDAPGroupMapper(session, realm);
//
//        // move groups
//        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.GROUP);
//        for (GroupModel child : group.getSubGroups()) {
//            if (ldapGroupMapper != null) {
//                ldapGroupMapper.moveGroup(child, null);
//            }
//            realm.moveGroup(child, null);
//
//            adminEvent.representation(ModelToRepresentation.toRepresentation(child, true))
//                    .resourcePath("groups", child.getId())
//                    .success();
//        }
//
//        adminEvent.representation(ModelToRepresentation.toRepresentation(group, true));
//
//        if (ldapGroupMapper != null) {
//            ldapGroupMapper.removeGroup(realm, group);
//        }
        realm.removeGroup(group);

        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
    }


    @DELETE
    @NoCache
    @Path("target/recycle")
    public void deleteGroupMoveMembersToRecycle() {
        this.auth.groups().requireManage(group);

//        LDAPStorageMapper ldapGroupMapper = getLDAPGroupMapper(session, realm);
//
//        if (ldapGroupMapper != null) {
//            ldapGroupMapper.removeGroup(realm, group);
//        }
        realm.removeGroup(group);

        adminEvent.resource(ResourceType.GROUP)
                .representation(ModelToRepresentation.toRepresentation(group, true))
                .operation(OperationType.DELETE)
                .resourcePath(session.getContext().getUri())
                .success();
    }

    @DELETE
    @NoCache
    @Path("target/{id}")
    public void deleteGroupMoveMembers(@PathParam("id") String id) {
        this.auth.groups().requireManage(group);

        GroupModel target = realm.getGroupById(id);
        if (target == null) {
            throw new NotFoundException(String.format("Could not find group by ID %s", id));
        }

        if (target.equals(group)) {
            throw new BadRequestException("Target group must not be same with the group to be deleted.");
        }

        GroupModel parentGroup = target.getParent();
        while (parentGroup != null) {
            if (parentGroup.equals(group)) {
                throw new BadRequestException("Target group must not be child or grandchild of the group to be deleted.");
            }
            parentGroup = parentGroup.getParent();
        }

        // move users
        adminEvent.operation(OperationType.CREATE)
                .resource(ResourceType.GROUP_MEMBERSHIP)
                .representation(ModelToRepresentation.toRepresentation(target, true));

        session.users().getGroupMembersStream(realm, group)
                .filter(u -> u.isMemberOf(target))
                .forEach(u -> {
                    u.joinGroup(target);
                    adminEvent.resourcePath("users", u.getId()).success();
                });

//        LDAPStorageMapper ldapGroupMapper = getLDAPGroupMapper(session, realm);

        // move groups
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.GROUP);
        for (GroupModel child : group.getSubGroupsStream().collect(Collectors.toSet())) {
//            if (ldapGroupMapper != null) {
//                ldapGroupMapper.moveGroup(child, target);
//            }
            realm.moveGroup(child, target);

            adminEvent.representation(ModelToRepresentation.toRepresentation(child, true))
                    .resourcePath("groups", child.getId())
                    .success();
        }

//        if (ldapGroupMapper != null) {
//            ldapGroupMapper.removeGroup(realm, group);
//        }
        realm.removeGroup(group);

        adminEvent.resource(ResourceType.GROUP)
                .representation(ModelToRepresentation.toRepresentation(group, true))
                .operation(OperationType.DELETE)
                .resourcePath(session.getContext().getUri())
                .success();
    }

    @GET
    @Path("children")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.GROUPS)
    @Operation( summary = "Return a paginated list of subgroups that have a parent group corresponding to the group on the URL")
    public Stream<GroupRepresentation> getSubGroups(
            @Parameter(description = "A String representing either an exact group name or a partial name") @QueryParam("search") String search,
            @Parameter(description = "Boolean which defines whether the params \"search\" must match exactly or not") @QueryParam("exact") Boolean exact,
            @Parameter(description = "The position of the first result to be returned (pagination offset).") @QueryParam("first") @DefaultValue("0") Integer first,
            @Parameter(description = "The maximum number of results that are to be returned. Defaults to 10") @QueryParam("max") @DefaultValue("10") Integer max,
            @Parameter(description = "Boolean which defines whether brief groups representations are returned or not (default: false)") @QueryParam("briefRepresentation") @DefaultValue("false") Boolean briefRepresentation) {
        this.auth.groups().requireView(group);
        boolean canViewGlobal = auth.groups().canView();
        return paginatedStream(
            group.getSubGroupsStream(search, exact, -1, -1)
            .filter(g -> canViewGlobal || auth.groups().canView(g)), first, max)
            .map(g -> GroupUtils.populateSubGroupCount(g, GroupUtils.toRepresentation(auth.groups(), g, !briefRepresentation)));
    }

    /**
     * Set or create child.  This will just set the parent if it exists.  Create it and set the parent
     * if the group doesn't exist.
     *
     * @param rep
     */
    @POST
    @Path("children")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.GROUPS)
    @Operation( summary = "Set or create child.", description = "This will just set the parent if it exists. Create it and set the parent if the group doesn’t exist.")
    public Response addChild(GroupRepresentation rep) {
        this.auth.groups().requireManage(group);

        if (rep.getGid() != null) {
            if (rep.getGid() <= 0) {
                throw ErrorResponse.error("Invalid group GID", Response.Status.BAD_REQUEST);
            }
        }

        String groupName = rep.getName();
        if (ObjectUtil.isBlank(groupName)) {
            throw ErrorResponse.error("Group name is missing", Response.Status.BAD_REQUEST);
        }

        try {
            Response.ResponseBuilder builder = Response.status(204);
            GroupModel child = null;
            if (rep.getId() != null) {
                child = realm.getGroupById(rep.getId());
                if (child == null) {
                    throw new NotFoundException("Could not find child by id");
                }
                if (!Objects.equals(child.getParentId(), group.getId())) {
                    realm.moveGroup(child, group);
                }
                adminEvent.operation(OperationType.UPDATE);
            } else {
                if (realm.isNasCompatible()) {
                    // check duplicate group name
                    Stream<GroupModel> groups = session.groups().getGroupsStream(realm, Stream.of(rep.getName()), 0, Integer.MAX_VALUE - 1);
                    if (groups.filter(g -> g.getName().equals(rep.getName())).count() != 0) {
                        throw ErrorResponse.exists("Group exists with same name");
                    }
                }

                child = realm.createGroup(groupName, group);
                updateGroup(rep, child, realm, session);
                URI uri = session.getContext().getUri().getBaseUriBuilder()
                        .path(AdminRoot.class)
                        .path(AdminRoot.class, "getRealmsAdmin")
                        .path(RealmsAdminResource.class, "getRealmAdmin")
                        .path(RealmAdminResource.class, "getGroups")
                        .path(GroupsResource.class, "getGroupById")
                        .build(realm.getName(), child.getId());
                builder.status(201).location(uri);
                rep.setId(child.getId());
                adminEvent.operation(OperationType.CREATE);

            }
            adminEvent.resourcePath(session.getContext().getUri()).representation(rep).success();

            GroupRepresentation childRep = GroupUtils.toRepresentation(auth.groups(), child, true);
            return builder.type(MediaType.APPLICATION_JSON_TYPE).entity(childRep).build();
        } catch (ModelDuplicateException e) {
            throw ErrorResponse.exists("Sibling group named '" + groupName + "' already exists.");
        }
    }

    @PUT
    @Path("parent/{id}")
    @NoCache
    public void setParent(@PathParam("id") String targetId) {
        this.auth.groups().requireManage(group);

        GroupModel target = realm.getGroupById(targetId);
        if (target == null) {
            throw new NotFoundException("Group with ID not found.");
        }

        GroupModel parent = group.getParent();
        if (parent != null && parent.equals(target)) {
            throw new BadRequestException("Target group is same with the original group.");
        }

//        LDAPStorageMapper ldapGroupMapper = getLDAPGroupMapper(session, realm);
//        if (ldapGroupMapper != null) {
//            ldapGroupMapper.moveGroup(group, target);
//        }
        realm.moveGroup(group, target);

        adminEvent.operation(OperationType.UPDATE)
                .resource(ResourceType.GROUP)
                .resourcePath(session.getContext().getUri())
                .representation(ModelToRepresentation.toRepresentation(group, true))
                .success();
    }

    @DELETE
    @Path("parent")
    @NoCache
    public void setNoParent() {
        this.auth.groups().requireManage(group);

        if (group.getParent() == null) {
            return;
        }

//        LDAPStorageMapper ldapGroupMapper = getLDAPGroupMapper(session, realm);
//        if (ldapGroupMapper != null) {
//            ldapGroupMapper.moveGroup(group, null);
//        }
        realm.moveGroup(group, null);

        adminEvent.operation(OperationType.UPDATE)
                .resource(ResourceType.GROUP)
                .resourcePath(session.getContext().getUri())
                .representation(ModelToRepresentation.toRepresentation(group, true))
                .success();
    }

    @GET
    @Path("parent/available")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<GroupRepresentation> getParentAvailable() {
        auth.groups().requireView(group);
        auth.groups().requireList();

        return ModelToRepresentation.toFilterGroupHierarchy(session, realm, group, true).collect(Collectors.toList());
    }

    @GET
    @Path("parent/available2")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<GroupRepresentation> getParentAvailable2() {
        auth.groups().requireView(group);
        auth.groups().requireList();

        return ModelToRepresentation.toFilterGroupHierarchy(session, realm, group,true).collect(Collectors.toList());
    }

    public static void updateGroup(GroupRepresentation rep, GroupModel model, RealmModel realm, KeycloakSession session) {
        String newName = rep.getName();
        if (newName != null) {
            String existingName = model.getName();
            if (!newName.equals(existingName)) {
                String previousPath = KeycloakModelUtils.buildGroupPath(model);

                model.setName(newName);

                String newPath = KeycloakModelUtils.buildGroupPath(model);

                GroupPathChangeEvent.fire(model, newPath, previousPath, session);
            }
        }

        if (rep.getDisplayName() != null) model.setDisplayName(rep.getDisplayName());

        if (rep.getAttributes() != null) {
            Set<String> attrsToRemove = new HashSet<>(model.getAttributes().keySet());
            attrsToRemove.removeAll(rep.getAttributes().keySet());
            for (Map.Entry<String, List<String>> attr : rep.getAttributes().entrySet()) {
                model.setAttribute(attr.getKey(), attr.getValue());
            }

            for (String attr : attrsToRemove) {
                model.removeAttribute(attr);
            }
        }
    }

    @Path("role-mappings")
    public RoleMapperResource getRoleMappings() {
        AdminPermissionEvaluator.RequirePermissionCheck manageCheck = () -> auth.groups().requireManage(group);
        AdminPermissionEvaluator.RequirePermissionCheck viewCheck = () -> auth.groups().requireView(group);
        return new RoleMapperResource(session, auth, group, adminEvent, manageCheck, viewCheck);

    }

    /**
     * Get users
     *
     * Returns a stream of users, filtered according to query parameters
     *
     * @param firstResult Pagination offset
     * @param maxResults Maximum results size (defaults to 100)
     * @param briefRepresentation Only return basic information (only guaranteed to return id, username, created, first and last name,
     *  email, enabled state, email verification state, federation link, and access.
     *  Note that it means that namely user attributes, required actions, and not before are not returned.)
     * @return a non-null {@code Stream} of users
     */
    @GET
    @NoCache
    @Path("members")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.GROUPS)
    @Operation( summary = "Get users Returns a stream of users, filtered according to query parameters")
    public Stream<UserRepresentation> getMembers(@Parameter(description = "Pagination offset") @QueryParam("first") Integer firstResult,
                                                 @Parameter(description = "Maximum results size (defaults to 100)") @QueryParam("max") Integer maxResults,
                                                 @Parameter(description = "Only return basic information (only guaranteed to return id, username, created, first and last name, email, enabled state, email verification state, federation link, and access. Note that it means that namely user attributes, required actions, and not before are not returned.)")
                                                     @QueryParam("briefRepresentation") Boolean briefRepresentation) {
        this.auth.groups().requireViewMembers(group);
        
        firstResult = firstResult != null ? firstResult : 0;
        maxResults = maxResults != null ? maxResults : Constants.DEFAULT_MAX_RESULTS;
        boolean briefRepresentationB = briefRepresentation != null && briefRepresentation;

        return session.users().getGroupMembersStream(realm, group, firstResult, maxResults)
                .map(user -> briefRepresentationB
                        ? ModelToRepresentation.toBriefRepresentation(user)
                        : ModelToRepresentation.toRepresentation(session, realm, user));
    }

    /**
     * Get users count
     *
     * @return
     */
    @GET
    @NoCache
    @Path("members/count")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Long> getMembersCount(@QueryParam("search") String search,
                                             @QueryParam("federatedOnly") @DefaultValue("false") boolean federatedOnly) {
        this.auth.groups().requireViewMembers(group);

        Long count;
        if (federatedOnly || !ObjectUtil.isBlank(search)) {
            Stream<UserModel> stream = session.users().getGroupMembersStream(realm, group);
            if (federatedOnly) {
                stream = stream.filter(u -> u.getFederationLink() != null && !u.getFederationLink().isEmpty());
            }
            if (!ObjectUtil.isBlank(search)) {
                CharSequence seq = search.trim();
                Predicate<UserModel> filter = ((Predicate<UserModel>) u -> u.getUsername().contains(seq))
                        .or(u -> u.getEmail() != null && u.getEmail().contains(seq))
                        .or(u -> u.getFirstName() != null && u.getFirstName().contains(seq))
                        .or(u -> u.getLastName() != null && u.getLastName().contains(seq))
                        .or(u -> u.getId().contains(seq));

                stream = stream.filter(filter);
            }

            count = stream.count();
        } else {
            count = session.users().getGroupMembersStream(realm, group).count();
        }

        return  Collections.singletonMap("count", count);
    }

    private Set<UserModel> allMembers(GroupModel group) {
        Set<UserModel> users = session.users()
                .getGroupMembersStream(realm, group)
                .collect(Collectors.toSet());

        group.getSubGroupsStream().forEach(subgroup -> users.addAll(allMembers(subgroup)));

        return users;
    }

    private static Set<String> all2MembersUsernames(KeycloakSession session, RealmModel realm, GroupModel group, Boolean nextOnly) {

        Set<String> users = session.users()
                .getGroupMembersUsernameInProvider(realm, group)
                .collect(Collectors.toSet());

        if (!nextOnly) {
            group.getSubGroupsStream().forEach(subgroup -> users.addAll(all2MembersUsernames(session, realm, subgroup, false)));
        }

        return users;
    }

    @GET
    @NoCache
    @Path("all-members")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<UsersResource.User>> getAllMembers(@QueryParam("search") String search,
                                                               @QueryParam("enabled") Boolean enabled,
                                                               @QueryParam("briefRepresentation") Boolean briefRepresentation) {
        this.auth.groups().requireViewMembers(group);

        List<Predicate<UserModel>> predicates = new ArrayList<>();
        if (!ObjectUtil.isBlank(search)) {
            CharSequence seq = search.trim();
            predicates.add(((Predicate<UserModel>) u -> UsersResource.searchUserByName(u, seq))
                    .or(u -> u.getEmail() != null && u.getEmail().contains(seq))
                    .or(u -> u.getId().contains(seq)));
        }
        if (enabled != null) {
            predicates.add(u -> u.isEnabled() == enabled);
        }

        Predicate<UserModel> filter = predicates.stream().reduce(u -> true, Predicate::and);
        Supplier<Stream<UserModel>> sup = () -> allMembers(group).stream().filter(filter);

        Map<String, String> idNameMap = new HashMap<>();
        sup.get().forEach(u -> {
            idNameMap.put(u.getId(), UsersResource.getName(u));
        });

        Map<String, String> pinyin = new HashMap<>();
        for (Map.Entry<String, String> entry : idNameMap.entrySet()) {
            try {
                pinyin.put(entry.getKey(), PinyinHelper.convertToPinyinString(entry.getValue(), "", PinyinFormat.WITH_TONE_NUMBER));
            } catch (PinyinException e) {
                e.printStackTrace();
                return Collections.emptyMap();
            }
        }

        Comparator<UserModel> sortByPinyin = Comparator.comparing(u -> Collections.unmodifiableMap(pinyin).get(u.getId()));
        Comparator<UserModel> sortByName = Comparator.comparing(u -> UsersResource.getName(u));
        Comparator<UserModel> sortByUserName = Comparator.comparing(u -> u.getUsername());

        boolean briefRep = briefRepresentation != null && briefRepresentation;
        Map<String, List<UsersResource.User>> results = new TreeMap<>(new UsersResource.IndexComparator());
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
                    List<UsersResource.User> users = results.get(s);
                    if (users == null) {
                        users = new LinkedList<>();
                    }
                    users.add(new UsersResource.User(rep));
                    results.put(s, users);
                });

        return results;
    }

    @GET
    @NoCache
    @Path("all-members/count")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Long> getAllMembersCount(@QueryParam("search") String search,
                                                @QueryParam("enabled") Boolean enabled) {
        this.auth.groups().requireViewMembers(group);

        List<Predicate<UserModel>> predicates = new ArrayList<>();
        if (!ObjectUtil.isBlank(search)) {
            CharSequence seq = search.trim();
            predicates.add(((Predicate<UserModel>) u -> UsersResource.searchUserByName(u, seq))
                    .or(u -> u.getEmail() != null && u.getEmail().contains(seq))
                    .or(u -> u.getId().contains(seq)));
        }
        if (enabled != null) {
            predicates.add(u -> u.isEnabled() == enabled);
        }

        Predicate<UserModel> filter = predicates.stream().reduce(u -> true, Predicate::and);
        return Collections.singletonMap("count", allMembers(group).stream().filter(filter).count());
    }

    @GET
    @NoCache
    @Path("all2-members")
    @Produces(MediaType.APPLICATION_JSON)
    public List<UsersResource.User> getAll2Members(@QueryParam("search") String search,
                                                   @QueryParam("first") Integer firstResult,
                                                   @QueryParam("max") Integer maxResults,
                                                   @QueryParam("nextOnly") @DefaultValue("true") Boolean nextOnly,
                                                   @QueryParam("briefRepresentation") Boolean briefRepresentation) {
        this.auth.groups().requireViewMembers(group);

        firstResult = firstResult == null ? 0 : firstResult;
        maxResults = maxResults == null ? Integer.MAX_VALUE : maxResults;

//        List<LDAPStorageProvider> ldapStorageProviders = getLDAPGroupProviders(session, realm);
//        List<UserStorageProviderModel> models = new ArrayList<>();
//        if (ldapStorageProviders.size() > 0) {
//            models = ldapStorageProviders.stream().map(p -> p.getModel()).collect(Collectors.toList());
//        }
//
//        List<UserStorageProviderModel> finalModels = models;

        boolean briefRep = briefRepresentation != null && briefRepresentation;

        List<UsersResource.User> results = new LinkedList<>();
        try {
            Set<String> usernames = all2MembersUsernames(session, realm, group, nextOnly);
            session.users().getUsersByUsernames(realm, usernames)
                    .filter(u -> search == null ||
                            search.trim().isEmpty() ||
                            u.getUsername().toLowerCase().contains(search.trim().toLowerCase()) ||
                            (u.getFirstName() != null && u.getFirstName().toLowerCase().contains(search.trim().toLowerCase())) ||
                            (u.getLastName() != null && u.getLastName().toLowerCase().contains(search.trim().toLowerCase())) ||
                            (u.getFirstName() != null && u.getLastName() != null && (u.getLastName()+u.getFirstName()).toLowerCase().contains(search.trim().toLowerCase())) ||
                            (u.getEmail() != null && u.getEmail().toLowerCase().contains(search.trim().toLowerCase())))
//                    .map(u -> {
//                            if (u.getFederationLink() == null || u.getFederationLink().isEmpty()) {
//                                return u;
//                            } else if (finalModels.size() > 0){
//                                for (UserStorageProviderModel model:finalModels) {
//                                    // TODO : not imported is error.
//                                    if (!model.isImportEnabled() || (model.isImportEnabled() && model.getId().equals(u.getFederationLink()))) {
//                                        return u;
//                                    }
//                                }
//                            }
//                            return null;
//                    })
//                    .filter(u -> u != null)
                    .skip(firstResult)
                    .limit(maxResults)
                    .forEach(u -> {
                        UserRepresentation rep = briefRep ? ModelToRepresentation.toBriefRepresentation(u) : ModelToRepresentation.toRepresentation(session, realm, u);
                        if (session.getProvider(BruteForceProtector.class).isTemporarilyDisabled(session, realm, u)) {
                            rep.setEnabled(false);
                        }
                        results.add(new UsersResource.User(rep));
                    });
//        } catch (LdapAuthenticationException e) {
//            throw new InternalServerErrorException("Error when trying to connect to LDAP, please check the ldap configuration.");
        } catch (Exception e) {
            throw new InternalServerErrorException("Error when trying to connect to LDAP, please check the ldap configuration.");
        }

        return results;
    }

    public static long getGroupAllMembersCount(KeycloakSession session, RealmModel realm, GroupModel group, Boolean nextOnly, String search){
//        List<LDAPStorageProvider> ldapStorageProviders = getLDAPGroupProviders(session, realm);
        List<UserStorageProviderModel> models = new ArrayList<>();
//        if (ldapStorageProviders.size() > 0) {
//            models = ldapStorageProviders.stream().map(p -> p.getModel()).collect(Collectors.toList());
//        }

        List<UserStorageProviderModel> finalModels = models;

        long result;
        try {
            result = session.users().getUsersByUsernames(realm, all2MembersUsernames(session, realm, group, nextOnly))
                    .filter(u -> search == null ||
                            search.trim().isEmpty() ||
                            u.getUsername().toLowerCase().contains(search.trim().toLowerCase()) ||
                            (u.getFirstName() != null && u.getFirstName().toLowerCase().contains(search.trim().toLowerCase())) ||
                            (u.getLastName() != null && u.getLastName().toLowerCase().contains(search.trim().toLowerCase())) ||
                            (u.getLastName() != null && u.getFirstName() != null && (u.getLastName()+u.getFirstName()).toLowerCase().contains(search.trim().toLowerCase())) ||
                            (u.getEmail() != null && u.getEmail().toLowerCase().contains(search.trim().toLowerCase())))
                    .map(u -> {
                        if (u.getFederationLink() == null || u.getFederationLink().isEmpty()) {
                            return u;
                        } else if (finalModels.size() > 0){
                            for (UserStorageProviderModel model:finalModels) {
                                // TODO : not imported is error.
                                if (!model.isImportEnabled() || (model.isImportEnabled() && model.getId().equals(u.getFederationLink()))) {
                                    return u;
                                }
                            }
                        }
                        return null;
                    })
                    .filter(u -> u != null)
                    .count();
//        } catch (LdapAuthenticationException e) {
//            throw new InternalServerErrorException("Error when trying to connect to LDAP, please check the ldap configuration.");
        } catch (Exception e) {
            throw new InternalServerErrorException("Error when trying to connect to LDAP, please check the ldap configuration.");
        }

        return result;
    }

    @GET
    @NoCache
    @Path("all2-members/count")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Long> getAll2MembersCount(@QueryParam("search") String search,
                                                 @QueryParam("nextOnly") @DefaultValue("true") Boolean nextOnly) {
        this.auth.groups().requireViewMembers(group);

        nextOnly = nextOnly == null ? false : nextOnly;

        long result;
        try {
            result = getGroupAllMembersCount(session, realm, group, nextOnly, search);
//        } catch (LdapAuthenticationException e) {
//            throw new InternalServerErrorException("Error when trying to connect to LDAP, please check the ldap configuration.");
        } catch (Exception e) {
            throw new InternalServerErrorException("Error when trying to connect to LDAP, please check the ldap configuration.");
        }
        return Collections.singletonMap("count", result);
    }

    /**
     * Return object stating whether client Authorization permissions have been initialized or not and a reference
     *
     * @return
     */
    @Path("management/permissions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.GROUPS)
    @Operation( summary = "Return object stating whether client Authorization permissions have been initialized or not and a reference")
    public ManagementPermissionReference getManagementPermissions() {
        ProfileHelper.requireFeature(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ);
        auth.groups().requireView(group);

        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
        if (!permissions.groups().isPermissionsEnabled(group)) {
            return new ManagementPermissionReference();
        }
        return toMgmtRef(group, permissions);
    }

    private ManagementPermissionReference toMgmtRef(GroupModel group, AdminPermissionManagement permissions) {
        ManagementPermissionReference ref = new ManagementPermissionReference();
        ref.setEnabled(true);
        ref.setResource(permissions.groups().resource(group).getId());
        ref.setScopePermissions(permissions.groups().getPermissions(group));
        return ref;
    }


    /**
     * Return object stating whether client Authorization permissions have been initialized or not and a reference
     *
     *
     * @return initialized manage permissions reference
     */
    @Path("management/permissions")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.GROUPS)
    @Operation( summary = "Return object stating whether client Authorization permissions have been initialized or not and a reference")
    public ManagementPermissionReference setManagementPermissionsEnabled(ManagementPermissionReference ref) {
        ProfileHelper.requireFeature(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ);
        auth.groups().requireManage(group);

        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
        permissions.groups().setPermissionsEnabled(group, ref.isEnabled());
        if (ref.isEnabled()) {
            return toMgmtRef(group, permissions);
        } else {
            return new ManagementPermissionReference();
        }
    }
}

