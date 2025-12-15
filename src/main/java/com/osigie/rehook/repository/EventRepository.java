package com.osigie.rehook.repository;

import com.osigie.rehook.domain.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {
    Optional<Event> findBySubscriptionIdAndIdempotencyKey(UUID subscriptionId, String idempotencyKey);

    void deleteBySubscriptionId(UUID id);
}
