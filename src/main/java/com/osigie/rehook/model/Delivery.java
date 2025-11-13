package com.osigie.rehook.model;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "deliveries")
public class Delivery extends BaseModel {

    @TenantId
    private String tenant;

    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<DeliveryAttempt> deliveryAttempts = new HashSet<>();


    public void addDeliveryAttempt(DeliveryAttempt deliveryAttempt) {
        deliveryAttempts.add(deliveryAttempt);
        deliveryAttempt.setDelivery(this);
    }
}
