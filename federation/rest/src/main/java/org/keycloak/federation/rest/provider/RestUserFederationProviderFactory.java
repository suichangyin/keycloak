package org.keycloak.federation.rest.provider;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.federation.rest.api.user.UserRepository;
import org.keycloak.federation.rest.model.AttributeConfigurations;
import org.keycloak.federation.rest.model.ConnConfigurations;
import org.keycloak.federation.rest.model.ExtraConfigurations;
import org.keycloak.federation.rest.model.HashAlgorithm;
import org.keycloak.federation.rest.model.ProviderConfig;
import org.keycloak.federation.rest.model.RestUser;
import org.keycloak.federation.rest.model.SignAlgorithm;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.UserCache;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.UserStorageUtil;
import org.keycloak.storage.user.ImportSynchronization;
import org.keycloak.storage.user.SynchronizationResult;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Remote user federation provider factory.
 * <p>
 * http://www.keycloak.org/docs/3.4/server_development/
 */
public class RestUserFederationProviderFactory implements UserStorageProviderFactory<RestUserFederationProvider>, ImportSynchronization {

    static Logger logger = Logger.getLogger(RestUserFederationProviderFactory.class);
    public static final String PROVIDER_NAME = "rest";
    public static final int URL_MIN_LENGHT = 10;
    public static final int PREFIX_MIN_LENGTH = 2;
    protected static final Set<String> OIDC_ATTRIBUTES;
    private static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm'Z'";
    private static final TimeZone TZ = TimeZone.getTimeZone("UTC");

    private Map<String, ProviderConfig> providerConfigPerInstance = new HashMap<>();

    static {
        // Get OIDC standard attributes
        Set<String> tmp = new HashSet<>();
        Field[] fs = IDToken.class.getDeclaredFields();
        for (Field f : fs) {
            try {
                tmp.add((String) f.get(null));
            } catch (IllegalAccessException | NullPointerException e) {
                logger.debug("Error processing IDToken attribute: " + f.getName());
            }
        }
        OIDC_ATTRIBUTES = tmp;
    }

    private synchronized ProviderConfig configure(ComponentModel model) {
        logger.infov("Creating configuration for model: id={0} name={1}", model.getId(), model.getName());

        ProviderConfig providerConfig = new ProviderConfig();
        SignAlgorithm signAlgorithm = SignAlgorithm.getByDescription(model.get(RestConstants.CLIENT_SIGN_ALGORITHM));
        providerConfig.setConnConfigurations(new ConnConfigurations(
                model.get(RestConstants.SERVER_URL),
                model.get(RestConstants.CLIENT_ID, ""),
                model.get(RestConstants.CLIENT_SECRET, ""),
                signAlgorithm,
                Boolean.parseBoolean(model.get(RestConstants.PROXY_ENABLED)),
                model.get(RestConstants.PROXY_HOST),
                Integer.parseInt(model.get(RestConstants.PROXY_PORT, "8080")),
                Integer.parseInt(model.get(RestConstants.PAGE_SIZE))
        ));

        providerConfig.setAttributeConfigurations(new AttributeConfigurations(
                model.get(RestConstants.PARENT_ATTRS),
                model.get(RestConstants.COUNT_ATTRS),
                model.get(RestConstants.USERNAME_ATTR),
                model.get(RestConstants.PASSWORD_ATTR),
                model.get(RestConstants.FIRSTNAME_ATTR),
                model.get(RestConstants.LASTNAME_ATTR),
                model.get(RestConstants.EMAIL_ATTR),
                model.get(RestConstants.CREATE_TIME_ATTR),
                model.get(RestConstants.MODIFY_TIME_ATTR),
                model.get(RestConstants.ENABLED_ATTR),
                model.get(RestConstants.EMAIL_VERIFIED_ATTR),
                model.get(RestConstants.GROUP_ATTR)
        ));

        HashAlgorithm hashAlgorithm = HashAlgorithm.getByDescription(model.get(RestConstants.PASSWORD_HASH_ALGORITHM));
        providerConfig.setExtraConfigurations(new ExtraConfigurations(
                model.get(RestConstants.ATTR_SYNC, false),
                model.get(RestConstants.ROLE_SYNC, false),
                model.get(RestConstants.PASSWORD_SYNC, false),
                hashAlgorithm,
                Integer.parseInt(model.get(RestConstants.PASSWORD_HASH_ITERATION, "27500")),
                model.get(RestConstants.ROLE_CLIENT_SYNC),
                model.get(RestConstants.PREFIX),
                model.get(RestConstants.UPPERCASE, false),
                model.get(RestConstants.UNCHECK_FEDERATION, false),
                model.get(RestConstants.NOT_CREATE_USERS, true),
                model.get(RestConstants.AUTO_LINKED_IDENTITY_PROVIDER, false),
                StringUtils.trim(model.get(RestConstants.IDENTITY_PROVIDER_ALIAS))
        ));

        return providerConfig;
    }

    public static String formatDate(Date date) {
        DateFormat DF = new SimpleDateFormat(DATETIME_FORMAT);// Quoted "Z" to indicate UTC, no timezone offset
        DF.setTimeZone(TZ);
        return DF.format(date);
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    public static <R> Predicate<R> not(Predicate<R> predicate) {
        return predicate.negate();
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ConfigurationProperties.PROPERTIES;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        ProviderConfig providerConfig = configure(config);
        AttributeConfigurations attributeConfig = providerConfig.getAttributeConfigurations();
        ExtraConfigurations extraConfig = providerConfig.getExtraConfigurations();

        try {
            Integer.parseInt(config.get(RestConstants.PROXY_PORT));
            Integer.parseInt(config.get(RestConstants.PASSWORD_HASH_ITERATION));
            Integer pageSize = Integer.parseInt(config.get(RestConstants.PAGE_SIZE));

            UserRepository userRepository = new UserRepository(providerConfig);
            userRepository.buildClient().getUsers(0, 1, false);

            if (StringUtils.isEmpty(attributeConfig.getUsernameAttr())) {
                throw new ComponentValidationException("Please full username attribute.");
            }
            if (pageSize < 1) {
                throw new ComponentValidationException("PageSize can not be less than 1!");
            }

            if (extraConfig.isAutoLinkedIdentityProvider()) {
                if (StringUtils.isEmpty(extraConfig.getIdentityProviderAlias())) {
                    throw new ComponentValidationException("Please full the identity provider alias.");
                }
                IdentityProviderModel providerModel = realm.getIdentityProviderByAlias(extraConfig.getIdentityProviderAlias());
                if (providerModel == null) {
                    throw new ComponentValidationException(
                            String.format("Can not find the provider(\"%s\"), please check the alias.", extraConfig.getIdentityProviderAlias())
                    );
                }
            }
        } catch (ComponentValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ComponentValidationException(e.getMessage(), e);
        }
//        final List<String> RESET_ACTIONS_LIST = Stream.of(UserModel.RequiredAction.values())
//                .map(Enum::name)
//                .collect(Collectors.toList());
//
//        List<String> notFound = resetActions.stream().filter(a -> !"".equals(a) && !RESET_ACTIONS_LIST.contains(a) && a.indexOf(".ftl") == -1).collect(Collectors.toList());
//        if (!notFound.isEmpty()) {
//            valid = false;
//            comment = "Please check actions: " + String.join("", notFound);
//        }
    }

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }

    @Override
    public String getHelpText() {
        return "This is a read-only user federation provider that can be used to sync user data one way from a remote REST API";
    }

    @Override
    public RestUserFederationProvider create(KeycloakSession session, ComponentModel model) {
//        ProviderConfig providerConfig = providerConfigPerInstance.computeIfAbsent(model.getId(), s -> configure(model));
//        return new DBFederationProvider(session, model, providerConfig.dataSourceProvider,
//                providerConfig.queryConfigurations, providerConfig.extraConfigurations);

        ProviderConfig providerConfig = providerConfigPerInstance.computeIfAbsent(model.getId(), s -> configure(model));

        UserRepository repository = new UserRepository(providerConfig);
        return new RestUserFederationProvider(session, model, repository, providerConfig.getExtraConfigurations());
    }

    @Override
    public SynchronizationResult sync(KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {
        return syncImpl(Optional.empty(), sessionFactory, realmId, model);
    }

    @Override
    public SynchronizationResult syncSince(Date date, KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {
        return syncImpl(Optional.of(date), sessionFactory, realmId, model);
    }

    private Set<RestUser> protector(Set<RestUser> list, final SynchronizationResult syncResult) {
        Set<RestUser> result = list.stream()
                .filter(Objects::nonNull)
                .filter(distinctByKey(u -> u.getEmail()))
                .filter(distinctByKey(u -> u.getUserName()))
                .collect(Collectors.toCollection(()
                        -> new TreeSet<>(Comparator.comparing(RestUser::getUserName))));

        list.stream()
                .distinct()
                .filter(not(result::contains))
                .collect(Collectors.toList())
                .forEach(u -> {
                    logger.warn("Ignored user: name->" + u.getUserName() + " email->" + u.getEmail());
                    syncResult.increaseFailed();
                });

        return result;

    }

    protected SynchronizationResult syncImpl(Optional<Date> date, KeycloakSessionFactory sessionFactory, final String realmId, final ComponentModel fedModel) {

        ProviderConfig providerConfig = providerConfigPerInstance.computeIfAbsent(fedModel.getId(), s -> configure(fedModel));
        UserRepository repository = new UserRepository(providerConfig);
        final SynchronizationResult syncResult = new SynchronizationResult();
        Set<RestUser> users;

        //Federation enabled
        if (date.isPresent()) {
            users = repository.getUpdatedUsers(formatDate(date.get()));
        } else {
            //Every
            users = repository.getUsers();
        }

        class BooleanHolder {
            private boolean value = true;
        }
        final BooleanHolder exists = new BooleanHolder();

        if (users != null) {
            logger.infof("[%s] Federation starting for '%s' users", fedModel.getName(), users.size());
            Set<RestUser> usersClean = protector(users, syncResult);
            for (final RestUser restUser : usersClean) {
                if (restUser.getUserName() != null && restUser.getEmail() != null) {
                    try {
                        // Process each user in it's own transaction to avoid global fail
                        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {
                            @Override
                            public void run(KeycloakSession session) {
                                RestUserFederationProvider restFedProvider = (RestUserFederationProvider) session.getProvider(UserStorageProvider.class, fedModel);
                                RealmModel currentRealm = session.realms().getRealm(realmId);

                                Boolean uncheck = providerConfig.getExtraConfigurations().getUncheckFederation();

                                String username = restUser.getUserName();
                                exists.value = true;
                                UserModel currentUser = UserStoragePrivateUtil.userLocalStorage(session)
                                        .getUserByUsername(currentRealm, username);

                                if (currentUser == null) {

                                    if (!providerConfig.getExtraConfigurations().is_not_create_users()) {

                                        UserModel storageCurrentUser = UserStoragePrivateUtil.userLocalStorage(session)
                                                .getUserByUsername(currentRealm, username);

                                        if (storageCurrentUser != null) {
                                            //He's in DB
                                            UserCache userCache = UserStorageUtil.userCache(session);
                                            if (userCache != null) {
                                                userCache.evict(currentRealm, storageCurrentUser);
                                            }
                                            logger.debugf("User %s exists. Evict him", username);

                                        } else {

                                            // Add new user to Keycloak
                                            exists.value = false;

                                            restFedProvider.importUserFromRest(session, currentRealm, restUser, uncheck);
                                            syncResult.increaseAdded();
                                        }

                                    } else {
                                        logger.debugf("notCreateUsers mode: Skip this users " + username);
                                    }
                                } else {
                                    //Uncheck mode ignore federation origin
                                    if ((fedModel.getId().equals(currentUser.getFederationLink()) || uncheck) && restUser.getUserName().equals(currentUser.getUsername())) {

                                        // Update keycloak user
                                        restFedProvider.updateUserFromRest(currentRealm, restUser, currentUser, uncheck);

                                        UserCache userCache = UserStorageUtil.userCache(session);
                                        if (userCache != null) {
                                            userCache.evict(currentRealm, currentUser);
                                        }

                                        logger.debugf("Updated user from REST: %s", currentUser.getUsername());
                                        syncResult.increaseUpdated();
                                    } else {
                                        logger.warnf("User '%s' is not updated during sync as he already exists in Keycloak database but is not linked to federation provider '%s'", username, fedModel.getName());
                                        syncResult.increaseFailed();
                                    }
                                }
                            }

                        });
                    } catch (ModelException me) {
                        logger.warn("Failed during import user from REST", me);
                        syncResult.increaseFailed();

                        // Remove user if we already added him during this transaction
                        if (!exists.value) {
                            KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

                                @Override
                                public void run(KeycloakSession session) {
                                    RealmModel currentRealm = session.realms().getRealm(realmId);

                                    if (restUser.getUserName() != null) {
                                        UserModel existing = UserStoragePrivateUtil.userLocalStorage(session)
                                                .getUserByUsername(currentRealm, restUser.getUserName());
                                        if (existing != null) {
                                            UserCache userCache = UserStorageUtil.userCache(session);
                                            if (userCache != null) {
                                                userCache.evict(currentRealm, existing);
                                            }
                                            UserStoragePrivateUtil.userLocalStorage(session)
                                                    .removeUser(currentRealm, existing);
                                        }
                                    }
                                }
                            });
                        }
                    } catch (IllegalStateException ie) {
                        logger.error("Failed during import user from REST", ie);
                        syncResult.increaseFailed();
                    }
                } else {
                    syncResult.increaseFailed();
                    logger.warnf("Missing attributes (user,email,password ?) for %s (%s)", restUser.getUserName() != null ? restUser.getUserName() : "", restUser.getEmail() != null ? restUser.getEmail() : "");
                }
            }
        } else {
            logger.errorf("Users is null. Check networking issue (see logs).");
        }

        logger.infof("[%s] Federation ended: '%s'", fedModel.getName(), syncResult.toString());

        return syncResult;
    }

    public void onCreate(KeycloakSession session, RealmModel realm, ComponentModel model) {
        providerConfigPerInstance.computeIfAbsent(model.getId(), s -> configure(model));
    }

    public void onUpdate(KeycloakSession session, RealmModel realm,
                         ComponentModel oldModel, ComponentModel newModel) {
        providerConfigPerInstance.put(newModel.getId(), configure(newModel));
    }
}
