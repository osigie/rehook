package com.osigie.rehook.util;

import com.osigie.rehook.domain.model.*;

import java.time.OffsetDateTime;
import java.util.UUID;

public class TestDataFactory {

    public static User createUser(String email) {
        return User.builder()
                .email(email)
                .password("encodedPassword")
                .tenant(createTestTenant())
                .build();
    }

    private static Tenant createTestTenant() {
        return Tenant.builder()
                .name("test-tenant-" + UUID.randomUUID())
                .build();
    }

    public static Endpoint createTestEndpoint(Subscription subscription) {
        Endpoint endpoint = Endpoint.builder()
                .url("https://example.com/webhook")
                .isActive(true)
                .build();
        endpoint.setSubscription(subscription);
        return endpoint;
    }

    public static Event createTestEvent(Subscription subscription, String payload) {
        return Event.builder()
                .payload(payload)
                .subscription(subscription)
                .receivedAt(OffsetDateTime.now())
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
    }

    public static Delivery createTestDelivery(Event event, Endpoint endpoint) {
        return Delivery.builder()
                .event(event)
                .endpoint(endpoint)
                .status(DeliveryStatusEnum.QUEUED)
                .retryCount(0)
                .build();
    }
}
