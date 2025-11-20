package com.osigie.rehook.domain.model;

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

    //add verification type and security
//    TODO:index, and think about security
    @Column(name = "secret")
    private String secret;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @TenantId
    private String tenant;


    @Builder
    public Endpoint(String url, boolean isActive, String secret) {
        this.url = url;
        this.isActive = isActive;
        this.secret = secret;

    }

}
