package com.osigie.rehook.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseModel {

    private String name;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "subscription")
    private Set<Endpoint> endpoints = new HashSet<Endpoint>();

    @TenantId
    private String tenant;

    @Builder
    public Subscription(String name) {
        this.name = name;
    }

    public void addEndpoint(Endpoint endpoint) {
        endpoints.add(endpoint);
        endpoint.setSubscription(this);
    }


    public void addEndpoint(List<Endpoint> endpoints) {
        endpoints.forEach(this::addEndpoint);
    }

//   TODO: add security type

}
