package org.keycloak.broker.cas.mappers;

import org.jboss.logging.Logger;
import org.keycloak.broker.cas.CasIdentityProvider;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.models.IdentityProviderMapperModel;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractAttributeMapper extends AbstractIdentityProviderMapper {

    protected static final Logger logger = Logger.getLogger(AbstractAttributeMapper.class);

    public static final String ATTRIBUTE = "attribute";
    public static final String ATTRIBUTE_VALUE = "attribute.value";

    public static List<String> getAttributeValue(
            final IdentityProviderMapperModel mapperModel, final BrokeredIdentityContext user) {
        @SuppressWarnings("unchecked")
        Map<String, List<String>> userAttributes =
                (Map<String, List<String>>) user.getContextData().get(CasIdentityProvider.USER_ATTRIBUTES);
        logger.debug("getAttributeValue attributes: " + userAttributes);
        return userAttributes.getOrDefault(
                mapperModel.getConfig().get(ATTRIBUTE), Collections.emptyList());
    }

    protected boolean hasAttributeValue(
            final IdentityProviderMapperModel mapperModel, final BrokeredIdentityContext context) {
        return CollectionUtil.collectionEquals(
                Collections.singletonList(mapperModel.getConfig().get(ATTRIBUTE_VALUE)),
                getAttributeValue(mapperModel, context));
    }
}
