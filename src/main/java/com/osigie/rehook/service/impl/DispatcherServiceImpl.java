package com.osigie.rehook.service.impl;

import com.osigie.rehook.domain.HttpResponse;
import com.osigie.rehook.domain.model.Delivery;
import com.osigie.rehook.domain.model.DeliveryAttempt;
import com.osigie.rehook.domain.model.DeliveryStatusEnum;
import com.osigie.rehook.repository.DeliveryRepository;
import com.osigie.rehook.service.DispatcherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
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

        DeliveryAttempt deliveryAttempt = DeliveryAttempt.builder()
                .delivery(delivery)
                .statusCode(response.code())
                .executedAt(start)
                .duration((int) Duration.between(start, OffsetDateTime.now()).toMillis())
                .responseBody(response.body())
                .responseHeaders(response.headers())
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


    public List<UUID> findRetries() {
        List<UUID> dueDeliveries = deliveryRepository
                .findAllByStatusAndNextRetryAtBefore(DeliveryStatusEnum.RETRY, OffsetDateTime.now());

        return dueDeliveries;
    }
}
