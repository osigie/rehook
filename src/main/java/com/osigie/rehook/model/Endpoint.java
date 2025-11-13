package com.osigie.rehook.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;



@Entity
@Table(name = "endpoints")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Endpoint extends BaseModel {


    @Column(name = "url")
    private String url;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = false;

//    TODO:index
    @Column(name = "secret")
    private String secret;

    @TenantId
    private String tenant;


    @Builder
    public Endpoint(String url, boolean isActive, String secret) {
        this.url = url;
        this.isActive = isActive;
        this.secret = secret;

    }

}
