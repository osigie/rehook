package com.osigie.rehook.service;

import com.osigie.rehook.domain.model.Delivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface DispatcherService {


    void dispatchDeliveriesAsync(List<UUID> deliveries);

    Page<UUID> findRetries(Pageable batchSize);

    Page<Delivery> listDeliveries(Pageable page);

    Delivery getDelivery(UUID id);

}
