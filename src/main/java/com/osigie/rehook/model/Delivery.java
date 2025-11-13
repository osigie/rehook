package com.osigie.rehook.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "deliveries")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Delivery extends BaseModel {

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id")
    private Event event;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "endpoint_id")
    private Endpoint endpoint;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private DeliveryStatusEnum status;

    @Column(name = "next_retry_at")
    private OffsetDateTime nextRetryAt;

    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<DeliveryAttempt> deliveryAttempts = new HashSet<>();

    @TenantId
    private String tenant;


    public void addDeliveryAttempt(DeliveryAttempt deliveryAttempt) {
        deliveryAttempts.add(deliveryAttempt);
        deliveryAttempt.setDelivery(this);
    }

    @Builder
    public Delivery(Event event, Endpoint endpoint, DeliveryStatusEnum status, OffsetDateTime nextRetryAt) {
        this.event = event;
        this.endpoint = endpoint;
        this.status = status;
        this.nextRetryAt = nextRetryAt;
    }
}
