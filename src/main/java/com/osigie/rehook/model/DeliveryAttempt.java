package com.osigie.rehook.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "delivery_attempts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryAttempt extends BaseModel {

    @Column(name = "status_code")
    private int statusCode;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    @Builder
    public DeliveryAttempt(int statusCode, Delivery delivery) {
        this.statusCode = statusCode;
        this.delivery = delivery;
    }
}
