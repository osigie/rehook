package com.osigie.rehook.service;

import com.osigie.rehook.domain.model.Endpoint;
import com.osigie.rehook.domain.model.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface SubscriptionService {

    Subscription save(Subscription subscription);

    Subscription update(UUID id, Subscription subscription);

    Subscription findById(UUID id);

    List<Subscription> findByName(String name);

    Page<Subscription> list(OffsetDateTime fromDate, OffsetDateTime toDate, Pageable pageable);

    Subscription findByIngestionId(String ingestionId);

    Subscription addEndpoints(List<Endpoint> endpoints, UUID id);

    Subscription updateEndpoint(Endpoint endpoint, UUID id, UUID endpointId);

    List<Endpoint> listEndpoints(UUID id);

    void deleteEndpoint(UUID id, UUID endpointId);

    void delete(UUID id);
}
