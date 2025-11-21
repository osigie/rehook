package com.osigie.rehook.domain.model;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tenant extends BaseModel {


    String name;

    @Builder
    public Tenant(String name, String description) {
        this.name = name;
    }

}
