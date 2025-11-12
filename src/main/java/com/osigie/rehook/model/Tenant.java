package com.osigie.rehook.model;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "events")
public class Tenant {

    @Id
    @GeneratedValue
    UUID id;

    String name;

    String email;
}
