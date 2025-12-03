package com.osigie.rehook.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "endpoints_auth")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EndpointAuth extends BaseModel {

    @Column(name = "auth_type")
    @Enumerated(EnumType.STRING)
    private AuthType authType = AuthType.NONE;

    @OneToOne
    @JoinColumn(name = "endpoint_id", nullable = false)
    private Endpoint endpoint;

    @Column(name = "api_key_name")
    private String apiKeyName;

    @Column(name = "api_key_value")
    private String apiKeyValue;

    @Column(name = "hmac_secret")
    private String hmacSecret;


    @Column(name = "basic_username")
    private String basicUsername;

    @Column(name = "basic_password")
    private String basicPassword;

    @TenantId
    private String tenant;

    @Builder
    public EndpointAuth(AuthType authType, String apiKeyName, String apiKeyValue, String hmacSecret, String basicUsername, String basicPassword) {
        this.authType = authType;
        this.apiKeyName = apiKeyName;
        this.apiKeyValue = apiKeyValue;
        this.hmacSecret = hmacSecret;
        this.basicUsername = basicUsername;
        this.basicPassword = basicPassword;
    }
}
