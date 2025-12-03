package com.osigie.rehook.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;


@Entity
@Table(name = "endpoints")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Endpoint extends BaseModel {


    @Column(name = "url")
    private String url;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = false;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "endpoint")
    private EndpointAuth endpointAuth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @TenantId
    private String tenant;

    public void addEndpointAuth(EndpointAuth endpointAuth) {
        if (endpointAuth != null) {
            endpointAuth.setEndpoint(this);
            this.endpointAuth = endpointAuth;
        }
    }

    @Builder
    public Endpoint(String url, boolean isActive, EndpointAuth endpointAuth) {
        this.url = url;
        this.isActive = isActive;
        addEndpointAuth(endpointAuth);
    }

}
