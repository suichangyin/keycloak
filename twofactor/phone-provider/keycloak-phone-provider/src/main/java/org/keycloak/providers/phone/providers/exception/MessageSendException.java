package org.keycloak.providers.phone.providers.exception;


import java.util.Objects;

public class MessageSendException extends Exception {
    private Integer statusCode = -1;
    private String errorCode = "";
    private String errorMessage = "";

    public MessageSendException() {
    }

    public MessageSendException(Throwable cause) {
        super(cause);
    }

    public MessageSendException(int statusCode, String errorCode, String errorMessage) {
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public MessageSendException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Integer statusCode, String errorCode, String errorMessage) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageSendException that = (MessageSendException) o;
        return Objects.equals(statusCode, that.statusCode) && Objects.equals(errorCode, that.errorCode) && Objects.equals(errorMessage, that.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statusCode, errorCode, errorMessage);
    }

    @Override
    public String toString() {
        return "MessageSendException{" +
                "statusCode=" + statusCode +
                ", errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
