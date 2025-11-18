package com.osigie.rehook.service;

import com.osigie.rehook.model.Endpoint;
import com.osigie.rehook.model.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface SubscriptionService {

    Subscription save(Subscription subscription);

    Subscription findById(UUID id);

    List<Subscription> findByName(String name);

    Page<Subscription> findByTenantId(String tenantId, Pageable pageable);

    Subscription findByIngestionId(String ingestionId);

    Subscription addEndpoints(List<Endpoint> endpoints, UUID id);

    List<Endpoint> listEndpoints(UUID id);
}
