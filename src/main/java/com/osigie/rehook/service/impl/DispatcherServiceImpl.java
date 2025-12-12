package com.osigie.rehook.service.impl;

import com.osigie.rehook.domain.HttpResponse;
import com.osigie.rehook.domain.model.Delivery;
import com.osigie.rehook.domain.model.DeliveryAttempt;
import com.osigie.rehook.domain.model.DeliveryStatusEnum;
import com.osigie.rehook.exception.ResourceNotFoundException;
import com.osigie.rehook.repository.DeliveryRepository;
import com.osigie.rehook.repository.specifications.DeliverySpecifications;
import com.osigie.rehook.service.DispatcherService;
import com.osigie.rehook.service.impl.HttpClient.HttpClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class DispatcherServiceImpl implements DispatcherService {

    private final DeliveryRepository deliveryRepository;
    private final HttpClientService httpClientService;

    public DispatcherServiceImpl(DeliveryRepository deliveryRepository, HttpClientService httpClientService) {
        this.deliveryRepository = deliveryRepository;
        this.httpClientService = httpClientService;
    }

    @Override
    @Async
    public void dispatchDeliveriesAsync(List<UUID> deliveries) {
        log.info("Dispatching deliveries async");
        deliveries.parallelStream().forEach(delivery -> {
            deliveryRepository.findById(delivery)
                    .ifPresentOrElse(this::processDeliveries, () -> log.error("Delivery with id {} not found", delivery));
        });
    }

    private void processDeliveries(Delivery delivery) {
        OffsetDateTime start = OffsetDateTime.now();

        HttpResponse response = httpClientService.send(delivery);
//TODO: investigate bug
        DeliveryAttempt deliveryAttempt = DeliveryAttempt.builder()
                .delivery(delivery)
                .statusCode(response.code())
                .executedAt(start)
                .duration((int) Duration.between(start, OffsetDateTime.now()).toMillis())
                .responseBody(response.body())
                .responseHeaders(new HashMap<>(response.headers()))
                .build();

        delivery.addDeliveryAttempt(deliveryAttempt);

        if (response.code() == 200) {
            delivery.setStatus(DeliveryStatusEnum.SUCCEEDED);
            delivery.setNextRetryAt(null);
            delivery.setRetryCount(0);
        } else {
            handleRetry(delivery);
        }

        deliveryRepository.save(delivery);
    }

    private void handleRetry(Delivery delivery) {
        int maxRetries = 3;
        int retryCount = delivery.getRetryCount();

        if (retryCount >= maxRetries) {
            delivery.setStatus(DeliveryStatusEnum.DLQ);
            delivery.setNextRetryAt(OffsetDateTime.now());
            return;
        }

        delivery.setRetryCount(retryCount + 1);

        OffsetDateTime nextRetryTime = calculateNextRetry(retryCount);
        delivery.setNextRetryAt(nextRetryTime);
        delivery.setStatus(DeliveryStatusEnum.RETRY);
    }

    private OffsetDateTime calculateNextRetry(int retryCount) {
        long baseSeconds = 2;
        long delay = (long) (baseSeconds * Math.pow(2, retryCount));
        return OffsetDateTime.now().plusSeconds(delay);
    }


    public Page<UUID> findRetries(Pageable batchSize) {
        return deliveryRepository
                .findAllByStatusAndNextRetryAtBefore(DeliveryStatusEnum.RETRY, OffsetDateTime.now(), batchSize);
    }

    @Override
    public Page<Delivery> listDeliveries(Pageable page, LocalDate fromDate, LocalDate toDate, DeliveryStatusEnum status) {
        Specification<Delivery> spec = DeliverySpecifications.withFilters(fromDate, toDate, status);
        return deliveryRepository.findAll(spec, page);
    }

    @Override
    public Delivery getDelivery(UUID id) {
        return deliveryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Delivery with id " + id + " not found"));
    }


}
