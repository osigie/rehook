package com.osigie.rehook.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TenantId;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "events", uniqueConstraints = @UniqueConstraint(
        name = "uk_event_subscription_idempotency",
        columnNames = {"subscription_id", "idempotency_key"}
))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event extends BaseModel {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "payload")
    private String payload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "headers")
    private Map<String, Object> headers;

    @Column(name = "received_at")
    private OffsetDateTime receivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "idempotency_key", length = 50)
    private String idempotencyKey;

    @TenantId
    private String tenant;

    @Builder
    public Event(String payload, Map<String, Object> headers, OffsetDateTime receivedAt, Subscription subscription, String idempotencyKey) {
        this.payload = payload;
        this.headers = headers;
        this.receivedAt = receivedAt;
        this.subscription = subscription;
        this.idempotencyKey = idempotencyKey;
    }

}
