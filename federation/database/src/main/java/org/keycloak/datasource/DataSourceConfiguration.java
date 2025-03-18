package org.keycloak.datasource;

import java.util.List;

import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import io.agroal.api.configuration.supplier.AgroalPropertiesReader;

public class DataSourceConfiguration {

    public static final String DB_PROVIDER = "dbProvider";
    public static final String SYNC_SQL = "syncSql";
    public static final String SYNC_SINCE_SQL = "syncSinceSql";
    public static final String SYNC_ONE_SQL = "syncOneSql";
    public static final String SYNC_ROLE_SQL = "syncRoleSql";

    public static List<ProviderConfigProperty> create() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(AgroalPropertiesReader.PROVIDER_CLASS_NAME)
                .label(DB_PROVIDER)
                .type(ProviderConfigProperty.LIST_TYPE)
                .options(RDBMS.getAllDescriptions())
                .defaultValue(RDBMS.MARIADB.getDesc())
                .helpText("Database provider")
                .add()
                .property()
                .name(AgroalPropertiesReader.JDBC_URL)
                .label(AgroalPropertiesReader.JDBC_URL)
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("jdbc:mariadb://localhost:3306/federationdb")
                .add()
                .property()
                .name(AgroalPropertiesReader.PRINCIPAL)
                .label(AgroalPropertiesReader.PRINCIPAL)
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(AgroalPropertiesReader.CREDENTIAL)
                .label(AgroalPropertiesReader.CREDENTIAL)
                .type(ProviderConfigProperty.PASSWORD)
                .secret(true)
                .add()
                .property()
                .name(SYNC_SQL)
                .label(SYNC_SQL)
                .defaultValue("SELECT username, email, email_verified, first_name, last_name, enabled, temp_password, required_actions, updated, custom_attr FROM db_user_users")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(SYNC_SINCE_SQL)
                .label(SYNC_SINCE_SQL)
                .defaultValue("SELECT username, email, email_verified, first_name, last_name, enabled, updated, custom_attr FROM db_user_users WHERE updated > ?")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(SYNC_ONE_SQL)
                .label(SYNC_ONE_SQL)
                .defaultValue("SELECT username, email, email_verified, first_name, last_name, enabled, updated, custom_attr FROM db_user_users WHERE username = ?")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(SYNC_ROLE_SQL)
                .label(SYNC_ROLE_SQL)
                .defaultValue("SELECT name FROM db_user_roles WHERE username = ?")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(AgroalPropertiesReader.INITIAL_SIZE)
                .label(AgroalPropertiesReader.INITIAL_SIZE)
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(0)
                .add()
                .property()
                .name(AgroalPropertiesReader.MIN_SIZE)
                .label(AgroalPropertiesReader.MIN_SIZE)
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(0)
                .add()
                .property()
                .name(AgroalPropertiesReader.MAX_SIZE)
                .label(AgroalPropertiesReader.MAX_SIZE)
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(20)
                .add()
                .property()
                .name(AgroalPropertiesReader.MAX_LIFETIME_S)
                .label(AgroalPropertiesReader.MAX_LIFETIME_S)
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(AgroalPropertiesReader.ACQUISITION_TIMEOUT_S)
                .label(AgroalPropertiesReader.ACQUISITION_TIMEOUT_S)
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(AgroalPropertiesReader.METRICS_ENABLED)
                .label(AgroalPropertiesReader.METRICS_ENABLED)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(Boolean.FALSE)
                .add()
                .build();
    }

}
