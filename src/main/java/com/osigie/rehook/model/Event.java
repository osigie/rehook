package com.osigie.rehook.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TenantId;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event extends BaseModel {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "payload")
    private Map<String, String> payload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "headers")
    private Map<String, String> headers;

    @Column(name = "received_at")
    private OffsetDateTime receivedAt;

    @TenantId
    private String tenant;

    @Builder
    public Event(Map<String, String> payload, Map<String, String> headers, OffsetDateTime receivedAt) {
        this.payload = payload;
        this.headers = headers;
        this.receivedAt = receivedAt;
    }

}
