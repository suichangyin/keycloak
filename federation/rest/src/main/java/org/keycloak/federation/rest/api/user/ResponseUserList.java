package org.keycloak.federation.rest.api.user;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class ResponseUserList {
    private Map<String, Object> properties = new HashMap<>();

    public ResponseUserList() {
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @JsonAnySetter
    public void handleUnknownField(String key, Object value) {
        this.properties.put(key, value);
    }

    public static class ResponseUser {
        private Map<String, Object> subProperties = new HashMap<>();

        public Map<String, Object> getSubProperties() {
            return subProperties;
        }

        public void setSubProperties(Map<String, Object> subProperties) {
            this.subProperties = subProperties;
        }

        @JsonAnySetter
        public void handleUnknownField(String key, Object value) {
            this.subProperties.put(key, value);
        }
    }

    public static void main(String[] args) {
//        String json = "{\"customKey1\": [{\"description\": \"user1 description\", \"email\": \"user1@x.com\", \"firstName\": \"user\", \"lastName\": \"1\", \"password\": \"user1password\", \"uid\": 1, \"username\": \"user1\"}, {\"description\": \"user2 description\", \"email\": \"user2@x.com\", \"firstName\": \"user\", \"lastName\": \"2\", \"password\": \"user2password\", \"uid\": 2, \"username\": \"user2\"}]}";
//
//        try {
//            ObjectMapper objectMapper = new ObjectMapper();
//            UserList userList = new UserList("customKey1");
//            Map<String, Object> parsedData = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
//
//            List<LinkedHashMap<String, Object>> usersData = (List<LinkedHashMap<String, Object>>) parsedData.get(userList.getTopLevelKey());
//
//            for (LinkedHashMap<String, Object> userData : usersData) {
//                User user = objectMapper.convertValue(userData, User.class);
//                System.out.println("User Properties:");
//                for (Map.Entry<String, Object> entry : user.getSubProperties().entrySet()) {
//                    System.out.println(entry.getKey() + ": " + entry.getValue());
//                }
//                System.out.println("----------------------");
//            }
//
//            System.out.println("UserList Properties:");
//            for (Map.Entry<String, Object> entry : userList.getProperties().entrySet()) {
//                if (!entry.getKey().equals(userList.getTopLevelKey())) {
//                    System.out.println(entry.getKey() + ": " + entry.getValue());
//                }
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

}
