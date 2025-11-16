package com.osigie.rehook.repository;

import com.osigie.rehook.model.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    List<Subscription> findByNameContainingIgnoreCase(String name);

    Page<Subscription> findByTenant(String tenantId, Pageable pageable);
}
