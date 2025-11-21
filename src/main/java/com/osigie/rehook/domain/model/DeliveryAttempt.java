package com.osigie.rehook.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

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

    @Column(name = "executed_at")
    private OffsetDateTime executedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "response_body")
    private Map<String, String> responseBody;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "response_headers")
    private Map<String, String> responseHeaders;

    @Column(name = "duration")
    private Integer duration;

    @Builder
    public DeliveryAttempt(int statusCode, Delivery delivery, OffsetDateTime executedAt, Map<String, String> responseBody, Map<String, String> responseHeaders, Integer duration) {
        this.statusCode = statusCode;
        this.delivery = delivery;
        this.executedAt = executedAt;
        this.responseBody = responseBody;
        this.responseHeaders = responseHeaders;
        this.duration = duration;

    }
}
