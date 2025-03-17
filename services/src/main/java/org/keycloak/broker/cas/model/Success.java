package org.keycloak.broker.cas.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.keycloak.broker.cas.jaxb.AttributesAdapter;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
public class Success implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @XmlElement(name = "user", namespace = "http://www.yale.edu/tp/cas")
    private String user;

    @XmlElement(name = "attributes", namespace = "http://www.yale.edu/tp/cas")
    @XmlJavaTypeAdapter(AttributesAdapter.class)
    private Map<String, List<String>> attributes = new HashMap<>();

    public String getUser() {
        return user;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(final Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return String.format("Success [user=%s, attributes=%s]", user, attributes);
    }
}
