package com.osigie.rehook.service.impl;

import com.osigie.rehook.model.Endpoint;
import com.osigie.rehook.model.Subscription;
import com.osigie.rehook.repository.EndpointRepository;
import com.osigie.rehook.repository.SubscriptionRepository;
import com.osigie.rehook.service.SubscriptionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {


    private final SubscriptionRepository subscriptionRepository;
    private final EndpointRepository endpointRepository;

    public SubscriptionServiceImpl(SubscriptionRepository subscriptionRepository, EndpointRepository endpointRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.endpointRepository = endpointRepository;
    }

    @Override
    public Subscription save(Subscription subscription) {
        return subscriptionRepository.save(subscription);
    }

    @Override
    public Subscription findById(UUID id) {
        return subscriptionRepository.findById(id).orElseThrow(() -> new RuntimeException("subscription not found"));
    }

    @Override
    public List<Subscription> findByName(String name) {
        return subscriptionRepository.findByNameContainingIgnoreCase(name);
    }

    @Override
    public Page<Subscription> findByTenantId(String tenantId, Pageable pageable) {
        return subscriptionRepository.findByTenant(tenantId, pageable);
    }

    @Override
    public Subscription addEndpoints(List<Endpoint> endpoints, UUID id) {
        Subscription subscription = subscriptionRepository.findById(id).orElseThrow(() -> new RuntimeException("subscription not found"));

        subscription.addEndpoint(endpoints);

        return subscriptionRepository.save(subscription);
    }

    @Override
    public List<Endpoint> listEndpoints(UUID id) {
        return endpointRepository.findBySubscriptionId(id);
    }

}
