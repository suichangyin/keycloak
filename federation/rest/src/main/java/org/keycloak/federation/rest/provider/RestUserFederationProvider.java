package org.keycloak.federation.rest.provider;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.federation.rest.api.user.UserRepository;
import org.keycloak.federation.rest.model.ExtraConfigurations;
import org.keycloak.federation.rest.model.HashAlgorithm;
import org.keycloak.federation.rest.model.RestUser;
import org.keycloak.models.ClientModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.UserStorageUtil;
import org.keycloak.storage.user.UserQueryProvider;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;


/**
 * Rest User federation to import users from remote user store
 */
public class RestUserFederationProvider implements
        UserStorageProvider,
        UserQueryProvider,
        CredentialInputUpdater,
        CredentialInputValidator {

    Logger logger = Logger.getLogger(RestUserFederationProvider.class);
    public static final int ROLE_MIN_LENGTH = 3;
    public static final String ACTION = "action";
    private static final String TEMPLATE = "template";
    private final Pattern p3 = Pattern.compile("\\((.*?)\\)");
    protected KeycloakSession session;
    protected UserStorageProviderModel model;
    protected UserRepository repository;
    protected Boolean upperCaseName;
    protected Boolean proxyOn;
    protected String prefix;
    protected Boolean roleIsSync;
    protected String roleClient;
    protected Boolean attributesIsSync;
    protected Boolean uncheckFederation;
    protected Boolean notCreateUsers;
    protected Boolean passwordIsSync;
    protected HashAlgorithm passwordAlgorithm;
    protected Integer passwordIteration;

    private Boolean autoLinkedIdentityProvider;
    private String identityProviderAlias;

    public RestUserFederationProvider(KeycloakSession session, ComponentModel model, UserRepository repository,
                                      ExtraConfigurations extra) {
        this.session = session;
        this.model = new UserStorageProviderModel(model);
        this.repository = repository;
        this.prefix = extra.getPrefix();
        this.roleIsSync = extra.getRoleSync();
        this.roleClient = extra.getRoleClient();
        this.proxyOn = repository.getProxyOn();
        this.upperCaseName = extra.getUppercase();
        this.attributesIsSync = extra.getAttrSync();
        this.uncheckFederation = extra.getUncheckFederation();
        this.notCreateUsers = extra.is_not_create_users();
        this.passwordIsSync = extra.getPasswordSync();
        this.passwordAlgorithm = extra.getPasswordHashAlgorithm();
        this.passwordIteration = extra.getPasswordHashIteration();
        this.autoLinkedIdentityProvider = extra.isAutoLinkedIdentityProvider();
        this.identityProviderAlias = extra.getIdentityProviderAlias();
    }

    /**
     * Convert only custom attributes (exclude standard claims)
     *
     * @param remoteName
     * @return converted name
     */
    private String convertRemoteName(String remoteName) {
        //see standard https://openid.net/specs/openid-connect-core-1_0.html
        String name = remoteName;

        if (!RestUserFederationProviderFactory.OIDC_ATTRIBUTES.contains(name)) {
            if (this.prefix != null && this.prefix.length() > 0) {
                name = this.prefix + "_" + remoteName.replaceFirst("^" + this.prefix + "_", "");
            }
            if (this.upperCaseName) {
                name = name.toUpperCase(Locale.US);
            }
        }
        return name;
    }


    @Override
    public void close() {
        //n/a
    }

    @Override
    public void preRemove(RealmModel realm) {
        //n/a
    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        //n/a
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        //n/a
    }

    protected UserModel importUserFromRest(KeycloakSession session, RealmModel realm, RestUser restUser, final Boolean uncheck) {
        String restUsername = restUser.getUserName();

        UserModel localUser = UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(realm, restUsername);
        if (localUser != null) {
            return localUser;
        } else {
            UserModel local = UserStoragePrivateUtil.userLocalStorage(session).addUser(realm, null, restUsername);
            logger.debugf("Imported new user from Rest to Keycloak DB. Username: [%s], Email: [%s] for Realm: [%s] ",
                    local.getUsername(), restUser.getEmail(), realm.getName());
            return proxy(realm, local, restUser, true, uncheck);
        }
    }

    protected UserModel updateUserFromRest(RealmModel realm, RestUser restUser, UserModel imported, final Boolean uncheck) {
        return proxy(realm, imported, restUser, false, uncheck);
    }

    private void attributeSynchronization(UserModel local, final RestUser restUser) {
        if (restUser.getAttributes() != null) {
            //clean attributes in local
            local.getAttributes().keySet().removeIf(item -> item.startsWith(this.prefix));

            Map<String, List<String>> map = restUser.getAttributes();
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                local.setAttribute(convertRemoteName(entry.getKey()), entry.getValue());
                logger.debugf("Remote attribute %s affected to %s", entry.getKey(), restUser.getUserName());
            }
        }
    }

    private void roleSynchronization(RealmModel realm, UserModel local, final RestUser restUser) {
        //Realm roles
        boolean isClientRoles = false;
        ClientModel client = null;

        if (this.roleClient != null && roleClient.length() > ROLE_MIN_LENGTH) {
            //Client roles
            client = realm.getClientByClientId(this.roleClient);
            if (client != null) {
                isClientRoles = true;
            } else {
                isClientRoles = false;
                logger.warnf("Client %s doesn't exist. Roles will be created as realm roles.", this.roleClient);
            }
        }


        if (restUser.getRoles() != null) {
            //clean roles in local
            if (isClientRoles) {
                local.getClientRoleMappingsStream(client).filter(item -> item.getName().startsWith(this.prefix));
            } else {
                local.getRealmRoleMappingsStream().filter(item -> item.getName().startsWith(this.prefix));
            }

            for (String role : restUser.getRoles()) {
                String roleNorm = convertRemoteName(role);
                RoleModel roleModel;
                if (isClientRoles) {
                    roleModel = client.getRole(roleNorm);
                    if (roleModel == null) {
                        //Create role
                        roleModel = client.addRole(roleNorm);
                        logger.infof("Remote role %s granted created", role);
                    }

                } else {
                    roleModel = realm.getRole(roleNorm);
                    if (roleModel == null) {
                        //Create role
                        roleModel = realm.addRole(roleNorm);
                        logger.infof("Remote role %s granted created", role);
                    }
                }


                //Apply role
                local.grantRole(roleModel);
                logger.debugf("Remote role %s granted to %s", role, restUser.getUserName());
            }
        }

    }

    private void mapper(RealmModel realm, UserModel local, final RestUser restUser) {
        //merge data from remote to local
        local.setUsername(restUser.getUserName().toLowerCase(Locale.US));
        local.setFirstName(restUser.getFirstName());
        local.setLastName(restUser.getLastName());
        local.setEmail(restUser.getEmail().toLowerCase(Locale.US));
        local.setEnabled(restUser.isEnabled());
        local.setEmailVerified(restUser.isEmailVerified());

        if (autoLinkedIdentityProvider) {
            autoLinkedToIdentityProvider(realm, local);
        }
    }

    private void autoLinkedToIdentityProvider(RealmModel realm, UserModel userAdapter) {
        if (StringUtils.isNotEmpty(identityProviderAlias)) {
            String providerId = realm.getIdentityProviderByAlias(StringUtils.trim(identityProviderAlias)).getProviderId();

            if (providerId != null) {
                UserModel userModel = UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(realm, userAdapter.getUsername());
                if (userModel != null) {
                    FederatedIdentityModel model1 = session.users().getFederatedIdentity(realm, userModel, providerId);
                    if (model1 == null) {
                        FederatedIdentityModel socialLink = new FederatedIdentityModel(providerId, userModel.getUsername(), userModel.getUsername());
                        session.users().addFederatedIdentity(realm, userModel, socialLink);
                        logger.debugf("User(%s) is auto linked with identity provider(%s).", userAdapter.getUsername(), providerId);
                    }
                } else {
                    logger.warnf("Can not find the user \"%s\".", userAdapter.getUsername());
                }
            } else {
                logger.errorf("Can not find the identity provider of alias \"%s\".", userAdapter.getUsername());
            }
        } else {
            logger.errorf("The identity provider alias is empty!");
        }
    }

    protected UserModel proxy(RealmModel realm, UserModel local, final RestUser restUser, final Boolean over,
                              final Boolean uncheck) {
        UserModel result = null;
        if (restUser != null) {
            if (!restUser.getEmail().equalsIgnoreCase(local.getEmail()) && !over) {
                throw new IllegalStateException(String.format("Local and remote users are not the same email : [%s != %s]", restUser.getEmail(), local.getEmail()));
            }

            //create restUser locally and set up relationship to this SPI
            if (over) {
                local.setFederationLink(model.getId());
            }

            mapper(realm, local, restUser);

            //pass roles along
            if (this.roleIsSync) {
                roleSynchronization(realm, local, restUser);
            }

            //pass attributes along
            if (this.attributesIsSync) {
                attributeSynchronization(local, restUser);
            }

            if (this.passwordIsSync) {
                passwordSynchronization(realm, local, restUser);
            }

            result = local;
        }
        return result;
    }

    private CredentialModel fillCredential(CredentialModel credentialModel, RestUser restUser) {
        credentialModel.setHashIterations(this.passwordIteration);
        credentialModel.setAlgorithm(this.passwordAlgorithm.getName());
        credentialModel.setType(UserCredentialModel.PASSWORD);
        credentialModel.setValue(restUser.getPassword());
        credentialModel.setCreatedDate(Time.currentTimeMillis());
        return credentialModel;
    }

    private void passwordSynchronization(RealmModel realm, UserModel local, RestUser restUser) {
        if (restUser.getPassword() != null) {

            Optional<CredentialModel> moOpt = UserStorageUtil.userFederatedStorage(session).getStoredCredentialsStream(realm, local.getId())
                    .filter(f -> f.getType().equals(UserCredentialModel.PASSWORD))
                    .findFirst();

            if (moOpt.isPresent()) {
                // Update credential
                local.credentialManager().updateStoredCredential(fillCredential(moOpt.get(), restUser));
            } else {
                // Create Credential
                local.credentialManager().createStoredCredential(fillCredential(moOpt.get(), restUser));
            }
        } else {
            logger.warnf("Missing password for: %s", restUser.getUserName());
        }
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return 0;
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        return Stream.empty();
    }

    @Override
    public Stream<String> getGroupMembersUsernameInProvider(RealmModel realm, GroupModel group) {
        return null;
    }

    @Override
    public Stream<String> getGroupMembersUsernameInProvider(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        return getGroupMembersStream(realm, group)
                .skip(firstResult)
                .limit(maxResults)
                .map(u -> u.getUsername());
    }

    @Override
    public Stream<UserModel> getUsersNoGroupStream(RealmModel realm) {
        return getUsersNoGroupStream(realm, 0, Integer.MAX_VALUE);
    }

    @Override
    public Stream<UserModel> getUsersNoGroupStream(RealmModel realm, int firstResult, int maxResults) {
        return UserStoragePrivateUtil.userLocalStorage(session).searchForUserStream(realm, Collections.emptyMap())
                .filter(u -> u.getFederationLink() != null && model.getId().equals(u.getFederationLink()))
                .sorted(Comparator.comparing(UserModel::getUsername))
                .skip(firstResult)
                .limit(maxResults);
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        return Stream.empty();
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return false;
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        return false;
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        throw new ReadOnlyException("Password update not supported!");
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input, boolean isTemporary, boolean ignorePasswordPolicy) {
        throw new ReadOnlyException("Password update not supported!");
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {

    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
        return Stream.empty();
    }
}
