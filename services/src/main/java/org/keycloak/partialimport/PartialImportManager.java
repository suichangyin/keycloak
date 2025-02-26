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

package org.keycloak.partialimport;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class manages the PartialImport handlers.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class PartialImportManager {
    private final List<PartialImport> partialImports = new ArrayList<>();

    private final PartialImportRepresentation rep;
    private final KeycloakSession session;
    private final RealmModel realm;

    public PartialImportManager(PartialImportRepresentation rep, KeycloakSession session,
                                RealmModel realm) {
        this.rep = rep;
        this.session = session;
        this.realm = realm;

        // Do not change the order of these!!!
        partialImports.add(new ClientsPartialImport());
        partialImports.add(new RolesPartialImport());
        partialImports.add(new IdentityProvidersPartialImport());
        partialImports.add(new IdentityProviderMappersPartialImport());
        partialImports.add(new GroupsPartialImport());
        partialImports.add(new UsersPartialImport());
    }

    public PartialImportResults saveResources() {
        PartialImportResults results = new PartialImportResults();

        for (PartialImport partialImport : partialImports) {
            partialImport.prepare(rep, realm, session);
        }

        Map<String, Set<String>> groupMemberships = session.users().searchForUserStream(realm, Collections.emptyMap())
                .collect(Collectors.toMap(
                        user -> user.getId(),
                        user -> user.getGroupsStream()
                                .map(ModelToRepresentation::buildGroupPath)
                                .collect(Collectors.toSet())
                ));

        for (PartialImport partialImport : partialImports) {
            partialImport.removeOverwrites(realm, session);
            results.addAllResults(partialImport.doImport(rep, realm, session));

            if (partialImport instanceof GroupsPartialImport) {
                groupMemberships.entrySet().stream()
                        .map(entry -> {
                            UserModel user = session.users().getUserById(realm, entry.getKey());
                            return user != null ? Map.entry(user, entry.getValue()) : null;
                        })
                        .filter(Objects::nonNull)
                        .forEach(entry -> {
                            UserModel user = entry.getKey();
                            entry.getValue().stream()
                                    .map(groupPath -> KeycloakModelUtils.findGroupByPath(session, realm, groupPath))
                                    .filter(Objects::nonNull)
                                    .filter(group -> !user.isMemberOf(group))
                                    .forEach(user::joinGroup);
                        });
            }
        }

        return results;
    }

}
