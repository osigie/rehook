package com.osigie.rehook.model;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue
    UUID id;

    String name;

    String description;

    @TenantId
    private String tenant;
}
