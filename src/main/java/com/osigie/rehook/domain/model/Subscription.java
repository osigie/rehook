package com.osigie.rehook.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "subscriptions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_subscription_name_tenant",
                        columnNames = {"name", "tenant"}
                )}

)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseModel {

    @Column(name = "name", nullable = false)
    private String name;

    //TODO: index
    @Column(name = "ingestion_id", nullable = false, length = 36)
    private String ingestionId;

    //add verification type and security


    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "subscription")
    private Set<Endpoint> endpoints = new HashSet<Endpoint>();

    @Column(name = "tenant", nullable = false, length = 20)
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
