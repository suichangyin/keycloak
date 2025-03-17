package org.keycloak.broker.cas.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.io.Serial;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "serviceResponse", namespace = "http://www.yale.edu/tp/cas")
public class ServiceResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @XmlElement(name = "authenticationFailure", namespace = "http://www.yale.edu/tp/cas")
    private Failure failure;

    @XmlElement(name = "authenticationSuccess", namespace = "http://www.yale.edu/tp/cas")
    private Success success;

    public Failure getFailure() {
        return failure;
    }

    public void setFailure(final Failure failure) {
        this.failure = failure;
    }

    public Success getSuccess() {
        return success;
    }

    public void setSuccess(final Success success) {
        this.success = success;
    }

    @Override
    public String toString() {
        return String.format("ServiceResponse [failure=%s, success=%s]", failure, success);
    }
}
