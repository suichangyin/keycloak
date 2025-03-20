package org.keycloak.providers.phone.providers.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "PHONE_MESSAGE_TOKEN_CODE")
@NamedQueries({
        @NamedQuery(
                name = "ongoingProcess",
                query = "FROM TokenCode t WHERE t.realmId = :realmId " +
                        "AND t.phoneNumber = :phoneNumber " +
                        "AND t.expiresAt >= :now AND t.type = :type"
        ),
        @NamedQuery(
                name = "processesSince",
                query = "FROM TokenCode t WHERE t.realmId = :realmId " +
                        "AND t.phoneNumber = :phoneNumber " +
                        "AND t.createdAt >= :date AND t.type = :type"
        ),
})
public class TokenCode {

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "REALM_ID", nullable = false)
    private String realmId;

    @Column(name = "PHONE_NUMBER", nullable = false)
    private String phoneNumber;

    @Column(name = "TYPE", nullable = false)
    private String type;

    @Column(name = "CODE", nullable = false)
    private String code;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_AT", nullable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "EXPIRES_AT", nullable = false)
    private Date expiresAt;

    @Column(name = "CONFIRMED", nullable = false)
    private Boolean confirmed;

    @Column(name = "BY_WHOM", nullable = true)
    private String byWhom;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(Boolean confirmed) {
        this.confirmed = confirmed;
    }

    public String getByWhom() {
        return byWhom;
    }

    public void setByWhom(String byWhom) {
        this.byWhom = byWhom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenCode tokenCode = (TokenCode) o;
        return Objects.equals(id, tokenCode.id) && Objects.equals(realmId, tokenCode.realmId) && Objects.equals(phoneNumber, tokenCode.phoneNumber) && Objects.equals(type, tokenCode.type) && Objects.equals(code, tokenCode.code) && Objects.equals(createdAt, tokenCode.createdAt) && Objects.equals(expiresAt, tokenCode.expiresAt) && Objects.equals(confirmed, tokenCode.confirmed) && Objects.equals(byWhom, tokenCode.byWhom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, realmId, phoneNumber, type, code, createdAt, expiresAt, confirmed, byWhom);
    }

    @Override
    public String toString() {
        return "TokenCode{" +
                "id='" + id + '\'' +
                ", realmId='" + realmId + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", type='" + type + '\'' +
                ", code='" + code + '\'' +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                ", confirmed=" + confirmed +
                ", byWhom='" + byWhom + '\'' +
                '}';
    }
}
