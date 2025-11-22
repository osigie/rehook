package com.osigie.rehook.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface DispatcherService {


    void dispatchDeliveriesAsync(List<UUID> deliveries);

    Page<UUID> findRetries(Pageable batchSize);
}
