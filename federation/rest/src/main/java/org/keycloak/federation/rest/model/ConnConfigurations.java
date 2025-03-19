package org.keycloak.federation.rest.model;

public class ConnConfigurations {
    private String url;
    private String appId;
    private String appSecret;
    private SignAlgorithm appSignAlgorithm;

    private Boolean proxyOn;
    private String proxyHost;
    private Integer proxyPort;
    private Integer pageSize;

    public ConnConfigurations(String url, String appId, String appSecret, SignAlgorithm appSignAlgorithm,
                              Boolean proxyOn, String proxyHost, Integer proxyPort, Integer pageSize) {
        this.url = url;
        this.appId = appId;
        this.appSecret = appSecret;
        this.appSignAlgorithm = appSignAlgorithm;
        this.proxyOn = proxyOn;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.pageSize = pageSize;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public Boolean getProxyOn() {
        return proxyOn;
    }

    public void setProxyOn(Boolean proxyOn) {
        this.proxyOn = proxyOn;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public Integer getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public SignAlgorithm getAppSignAlgorithm() {
        return appSignAlgorithm;
    }

    public void setAppSignAlgorithm(SignAlgorithm appSignAlgorithm) {
        this.appSignAlgorithm = appSignAlgorithm;
    }
}
