package com.osigie.rehook.domain.model;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tenant extends BaseModel {


    @Column(unique = true, nullable = false, length = 50, name = "name")
    private String name;

    @Builder
    public Tenant(String name) {
        this.name = name;
    }

}
