package org.keycloak.providers.phone.providers.sender;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.RealmModel;
import org.keycloak.providers.phone.providers.constants.TokenCodeType;
import org.keycloak.providers.phone.providers.exception.MessageSendException;
import org.keycloak.providers.phone.providers.spi.MessageSenderService;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DatatomSmsSenderServiceProvider implements MessageSenderService {

    private final static Logger logger = Logger.getLogger(DatatomSmsSenderServiceProvider.class);
    private final Config.Scope config;
    private final RealmModel realm;
//    private static final String SERVER_URL = "https://sms.datatom.com/falcon-oc/v1/sms/send-msg";
    private static final String SERVER_URL = "http://127.0.0.1/hms/v1/sms/send-sms";

    public DatatomSmsSenderServiceProvider(Config.Scope config, RealmModel realm) {
        this.config = config;
        this.realm = realm;
    }

    @Override
    public void sendSmsMessage(TokenCodeType type, String phoneNumber, String code, int expires) throws MessageSendException {
        String authorization = "Basic ZGF0YXRvbS1jbG91ZDo2MWU1MmYyYS1mOGQwLTQyMmQtYWU2MS05NThmZWMzMzBlNmE=";

        Map<String, Object> jsonBody = new HashMap<>();
        jsonBody.put("phone_numbers", new String[]{phoneNumber});
        jsonBody.put("sms_api_type", "VERIFY");

        Map<String, Object> verifyBody = new HashMap<>();
        verifyBody.put("code", code);
        verifyBody.put("expires", expires);

        jsonBody.put("msg_verify", verifyBody);

        try {
            String jsonString = new ObjectMapper().writeValueAsString(jsonBody);

            HttpPost httpPost = new HttpPost(SERVER_URL);
            httpPost.setEntity(new StringEntity(jsonString));

            HttpResponse response = HttpClientBuilder.create().setDefaultHeaders(Arrays.asList(
                            new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8"),
                            new BasicHeader("Authorization", authorization)))
                    .build().execute(httpPost);

            int responseCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity(), "utf-8");
            int responseBodyCode = new ObjectMapper().readTree(responseBody).get("code").asInt();

            if (responseCode != 200 || responseBodyCode != 200) {
                logger.warnf("Sending sms failed in datatom: %s, %s", responseCode, responseBody);
                throw new MessageSendException();
            } else {
                logger.infof("Sending sms success in datatom: %s, %s", responseCode, responseBody);
            }

        } catch (UnsupportedEncodingException e) {
            logger.error("Sending sms failed with UnsupportedEncodingException.", e);
            throw new MessageSendException();
        } catch (JsonProcessingException e) {
            logger.error("Sending sms failed with JsonProcessingException.", e);
            throw new MessageSendException();
        } catch (ClientProtocolException e) {
            logger.error("Sending sms failed with ClientProtocolException.", e);
            throw new MessageSendException();
        } catch (IOException e) {
            logger.error("Sending sms failed with IOException.", e);
            throw new MessageSendException();
        }
    }

    @Override
    public void close() {

    }
}
