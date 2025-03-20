package org.keycloak.providers.phone.providers.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.keycloak.Config;
import org.keycloak.models.RealmModel;
import org.keycloak.providers.phone.providers.constants.TokenCodeType;
import org.keycloak.providers.phone.providers.exception.MessageSendException;
import org.keycloak.providers.phone.providers.spi.MessageSenderService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class WechatSmsSenderServiceProvider implements MessageSenderService {

    private final Config.Scope config;
    private final RealmModel realm;
    private static final String SERVER_URL = "http://127.0.0.1/wechat/api/v1/sendVerificationCode";

    public WechatSmsSenderServiceProvider(Config.Scope config, RealmModel realm) {
        this.config = config;
        this.realm = realm;
    }

    @Override
    public void sendSmsMessage(TokenCodeType type, String phoneNumber, String code, int expires) throws MessageSendException {
        String authorization = "Basic ZGF0YXRvbS1jbG91ZDo2MWU1MmYyYS1mOGQwLTQyMmQtYWU2MS05NThmZWMzMzBlNmE=";
        HttpPost httpPost = new HttpPost(SERVER_URL);

        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> jsonBody = new HashMap<>();
        jsonBody.put("userId", phoneNumber);
        jsonBody.put("code", code);
        jsonBody.put("validity", expires);

        int responseCode = 500;
        try {
            String jsonString = objectMapper.writeValueAsString(jsonBody);
            httpPost.setEntity(new StringEntity(jsonString));

            HttpResponse response = HttpClientBuilder.create().setDefaultHeaders(Arrays.asList(
                            new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8"),
                            new BasicHeader("Authorization", authorization)))
                    .build().execute(httpPost);

            responseCode = response.getStatusLine().getStatusCode();
        } catch (Exception e) {
            e.printStackTrace();
            throw new MessageSendException(e);
        }

        if (responseCode != 200) {
            throw new MessageSendException();
        }
    }

    @Override
    public void close() {

    }
}
