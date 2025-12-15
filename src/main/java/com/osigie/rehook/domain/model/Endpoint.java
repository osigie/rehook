package com.osigie.rehook.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

import java.util.HashSet;
import java.util.Set;


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

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "endpoint", orphanRemoval = true)
    private EndpointAuth endpointAuth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @OneToMany(mappedBy = "endpoint", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Delivery> deliveries = new HashSet<>();

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
