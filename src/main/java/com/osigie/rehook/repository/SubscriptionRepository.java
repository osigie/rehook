package com.osigie.rehook.repository;

import com.osigie.rehook.domain.model.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    List<Subscription> findByNameContainingIgnoreCase(String name);

    Page<Subscription> findByTenant(String tenantId, Pageable pageable);

    Optional<Subscription> findByIngestionId(String ingestionId);

    Page<Subscription> findAll(Specification<Subscription> spec, Pageable pageable);
}
