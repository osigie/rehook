package com.osigie.rehook.service.impl;

import com.osigie.rehook.configuration.tenancy.TenantContext;
import com.osigie.rehook.domain.model.Endpoint;
import com.osigie.rehook.domain.model.EndpointAuth;
import com.osigie.rehook.domain.model.Subscription;
import com.osigie.rehook.exception.ConflictException;
import com.osigie.rehook.exception.ResourceNotFoundException;
import com.osigie.rehook.repository.DeliveryRepository;
import com.osigie.rehook.repository.EndpointRepository;
import com.osigie.rehook.repository.SubscriptionRepository;
import com.osigie.rehook.repository.specifications.SubscriptionSpecifications;
import com.osigie.rehook.service.SubscriptionService;
import jakarta.persistence.EntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {


    private final SubscriptionRepository subscriptionRepository;
    private final EndpointRepository endpointRepository;
    private final DeliveryRepository deliveryRepository;

    public SubscriptionServiceImpl(SubscriptionRepository subscriptionRepository, EndpointRepository endpointRepository, DeliveryRepository deliveryRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.endpointRepository = endpointRepository;
        this.deliveryRepository = deliveryRepository;
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
    public Page<Subscription> list(OffsetDateTime fromDate, OffsetDateTime toDate, Pageable pageable) {
        Specification<Subscription> spec = SubscriptionSpecifications.withFilters(fromDate, toDate);
        return subscriptionRepository.findAll(spec, pageable);
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
    public Subscription updateEndpoint(Endpoint endpoint, UUID id, UUID endpointId) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("subscription not found"));

        Endpoint existingEndpoint = endpointRepository.findByIdAndSubscriptionId(endpointId, id).orElseThrow(() -> new ResourceNotFoundException("Endpoint not found"));

        EndpointAuth existingAuth = existingEndpoint.getEndpointAuth();
        EndpointAuth newAuth = endpoint.getEndpointAuth();

        if (existingAuth != null && newAuth != null) {
            existingAuth.setAuthType(newAuth.getAuthType());
            existingAuth.setApiKeyName(newAuth.getApiKeyName());
            existingAuth.setApiKeyValue(newAuth.getApiKeyValue());
            existingAuth.setBasicUsername(newAuth.getBasicUsername());
            existingAuth.setBasicPassword(newAuth.getBasicPassword());
            existingAuth.setHmacSecret(newAuth.getHmacSecret());
        } else if (newAuth != null) {
            existingEndpoint.addEndpointAuth(newAuth);
        }

        existingEndpoint.setUrl(endpoint.getUrl());
        existingEndpoint.setActive(endpoint.isActive());

        subscription.addEndpoint(existingEndpoint);

        endpointRepository.save(existingEndpoint);
        return subscription;
    }

    @Override
    public List<Endpoint> listEndpoints(UUID id) {
        return endpointRepository.findBySubscriptionId(id);
    }

    @Override
    @Transactional
    public void deleteEndpoint(UUID id, UUID endpointId) {

        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("subscription not found"));

        Endpoint existingEndpoint = endpointRepository.findByIdAndSubscriptionId(endpointId, id).orElseThrow(() -> new ResourceNotFoundException("Endpoint not found"));

        subscription.removeEndpoint(existingEndpoint);

        endpointRepository.save(existingEndpoint);
        endpointRepository.delete(existingEndpoint);
    }

}
