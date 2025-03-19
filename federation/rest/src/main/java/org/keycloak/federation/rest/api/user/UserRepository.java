package org.keycloak.federation.rest.api.user;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.keycloak.federation.rest.api.user.object.service.UserServiceOdataObject;
import org.keycloak.federation.rest.model.AttributeConfigurations;
import org.keycloak.federation.rest.model.ConnConfigurations;
import org.keycloak.federation.rest.model.ProviderConfig;
import org.keycloak.federation.rest.model.RestUser;
import org.keycloak.federation.rest.model.SignAlgorithm;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientRequestFilter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Remote repository to load remote user data from UserService using REST
 */
public class UserRepository implements UserMapper {

    public static Logger logger = Logger.getLogger(UserRepository.class);
    private String url;
    private String appId;
    private String appSecret;
    private SignAlgorithm signAlgorithm;
    private Boolean proxyOn;
    private String host;
    private Integer port;
    private Integer pageSize;
    private ConnConfigurations connConfig;
    private AttributeConfigurations attributeConfig;

    public UserRepository(ProviderConfig providerConfig) {
        this.connConfig = providerConfig.getConnConfigurations();
        this.attributeConfig = providerConfig.getAttributeConfigurations();
        this.url = connConfig.getUrl();
        this.appId = connConfig.getAppId();
        this.appSecret = connConfig.getAppSecret();
        this.signAlgorithm = connConfig.getAppSignAlgorithm();
        this.proxyOn = connConfig.getProxyOn();
        this.host = connConfig.getProxyHost();
        this.port = connConfig.getProxyPort();
        this.pageSize = connConfig.getPageSize();
    }

    public UserServiceOdataObject buildClient() {
        ResteasyClientBuilder builder = new ResteasyClientBuilderImpl();

        if (proxyOn) {
            builder.defaultProxy(host, port);
        }

        if (StringUtils.isNotEmpty(appId) && StringUtils.isNotEmpty(appSecret)) {
            Long now = System.currentTimeMillis();
            String origin = String.format("%s%s%s", appId, appSecret, now);

            builder.register((ClientRequestFilter) context -> {
                String sign = "";
                switch (signAlgorithm) {
                    case MD5_BASE64:
                        sign = Base64.getEncoder().encodeToString(
                                DigestUtils.md5Hex(origin.getBytes(StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8)
                        );
                }

                context.getHeaders().add("appId", appId);
                context.getHeaders().add("appSecret", appSecret);
                context.getHeaders().add("timestamp", now);
                context.getHeaders().add("sign", sign);
            });
        }


        ResteasyClient client = builder.disableTrustManager().build();
        ResteasyWebTarget target = client.target(url);

        return target
                .proxyBuilder(UserServiceOdataObject.class)
                .classloader(UserServiceOdataObject.class.getClassLoader())
                .build();
    }

    public Set<RestUser> getUsers() {
        Set<RestUser> result = new HashSet<>();
        try {
            String responseBody = buildClient().getUsers(0, pageSize, true).body();
            result = convertBodyToObject(attributeConfig, responseBody);

            int totalPages = getTotalPage(responseBody);
            if (totalPages > 1) {
                for (int i = 2; i <= totalPages; i++) {
                    responseBody = buildClient().getUsers((i - 1) * pageSize, pageSize, true).body();
                    Set<RestUser> added = convertBodyToObject(attributeConfig, responseBody);
                    logger.debugf("Process page: %s and adding %s users.", i, added.size());
                    result.addAll(added);
                }
            }
        } catch (WebApplicationException e) {
            logger.warn("Received a non OK answer from upstream migration service", e);
        }

        return result;
    }

    public Set<RestUser> convertBodyToObject(AttributeConfigurations config, String response) {
        logger.debugf("Rest response body: %s", response);
        Set<RestUser> restUsers = new HashSet<>();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> parsedData = objectMapper.readValue(response,
                    new TypeReference<Map<String, Object>>() {
                    }
            );

            List<LinkedHashMap<String, Object>> usersData = (List<LinkedHashMap<String, Object>>) parsedData.get(config.getParentAttr());

            if (usersData != null) {
                for (LinkedHashMap<String, Object> userData : usersData) {
                    ResponseUserList.ResponseUser responseUser = objectMapper.convertValue(userData, ResponseUserList.ResponseUser.class);
                    Map<String, Object> objectMap = responseUser.getSubProperties();
                    restUsers.add(new RestUser(config, objectMap));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return restUsers;
    }

    public Integer getUsersCount(AttributeConfigurations config, String response) {
        logger.debugf("Rest response body: %s", response);

        Integer result = -1;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> parsedData = objectMapper.readValue(response,
                    new TypeReference<Map<String, Object>>() {
                    }
            );

            Object countStr = parsedData.getOrDefault(config.getCountAttr(), -1);
            if (countStr instanceof Integer) {
                return (Integer) countStr;
            } else if (countStr instanceof String) {
                return Integer.valueOf((String) countStr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public Set<RestUser> getUpdatedUsers(String date) {
        Set<RestUser> result = new HashSet<>();
        try {
            String responseBody = buildClient().getUpdatedUsers(0, pageSize, true).body();
            result = convertBodyToObject(attributeConfig, responseBody);

            int totalPages = getTotalPage(responseBody);
            if (totalPages > 1) {
                for (int i = 2; i <= totalPages; i++) {
                    responseBody = buildClient().getUpdatedUsers((i - 1) * pageSize, pageSize, true).body();
                    Set<RestUser> added = convertBodyToObject(attributeConfig, responseBody);
                    logger.debugf("Process page: %s and adding %s users.", i, added.size());
                    result.addAll(added);
                }
            }
        } catch (WebApplicationException e) {
            logger.warn("Received a non OK answer from upstream migration service", e);
        }

        return result;
    }

    private int getTotalPage(String responseBody) {
//        int result = 0;
//        if (response != null && response.totalPages() != null && response.page() != null) {
//            try {
//                int totalPages = Integer.parseInt(response.totalPages());
//                if (totalPages > Integer.parseInt(response.page())) {
//                    result = totalPages;
//                }
//            } catch (NumberFormatException e) {
//                logger.warn("Paging header not well formed", e);
//            }
//        }
        Integer count = getUsersCount(attributeConfig, responseBody);
        if (count > 1) {
            return count / pageSize;
        } else {
            return 1;
        }
    }

    public String getUrl() {
        return url;
    }

    public String getAppId() {
        return appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public Boolean getProxyOn() {
        return proxyOn;
    }
}
