package com.osigie.rehook.service.impl;

import com.osigie.rehook.configuration.tenancy.TenantContext;
import com.osigie.rehook.domain.model.Endpoint;
import com.osigie.rehook.domain.model.Subscription;
import com.osigie.rehook.exception.ConflictException;
import com.osigie.rehook.exception.ResourceNotFoundException;
import com.osigie.rehook.repository.EndpointRepository;
import com.osigie.rehook.repository.SubscriptionRepository;
import com.osigie.rehook.service.SubscriptionService;
import jakarta.persistence.EntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {


    private final SubscriptionRepository subscriptionRepository;
    private final EndpointRepository endpointRepository;
    private final EntityManager entityManager;

    public SubscriptionServiceImpl(SubscriptionRepository subscriptionRepository, EndpointRepository endpointRepository, EntityManager entityManager) {
        this.subscriptionRepository = subscriptionRepository;
        this.endpointRepository = endpointRepository;
        this.entityManager = entityManager;
    }

    @Override
    public Subscription save(Subscription subscription) {
        try {

            String ingestId = UUID.randomUUID().toString();
            subscription.setIngestionId(ingestId);
            subscription.setTenant(TenantContext.get().getTenantId());
            return subscriptionRepository.save(subscription);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Subscription already exists");
        }
    }

    @Override
    public Subscription findById(UUID id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("subscription not found"));
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
    public Subscription findByIngestionId(String ingestionId) {
        return subscriptionRepository.findByIngestionId(ingestionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
    }


    @Override
    public Subscription addEndpoints(List<Endpoint> endpoints, UUID id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("subscription not found"));
        subscription.addEndpoint(endpoints);
        return subscriptionRepository.save(subscription);
    }

    @Override
    public List<Endpoint> listEndpoints(UUID id) {
        return endpointRepository.findBySubscriptionId(id);
    }

}
