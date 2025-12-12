package com.osigie.rehook.service;

import com.osigie.rehook.domain.model.Delivery;
import com.osigie.rehook.domain.model.DeliveryStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface DispatcherService {


    void dispatchDeliveriesAsync(List<UUID> deliveries);

    Page<UUID> findRetries(Pageable batchSize);

    Page<Delivery> listDeliveries(Pageable page, OffsetDateTime fromDate, OffsetDateTime toDate, DeliveryStatusEnum status);

    Delivery getDelivery(UUID id);

}
