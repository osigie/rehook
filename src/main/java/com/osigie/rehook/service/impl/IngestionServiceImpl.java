package com.osigie.rehook.service.impl;

import com.osigie.rehook.domain.DeliveriesCreatedEvent;
import com.osigie.rehook.domain.model.Delivery;
import com.osigie.rehook.domain.model.DeliveryStatusEnum;
import com.osigie.rehook.domain.model.Event;
import com.osigie.rehook.domain.model.Subscription;
import com.osigie.rehook.repository.DeliveryRepository;
import com.osigie.rehook.repository.EventRepository;
import com.osigie.rehook.service.IngestionService;
import com.osigie.rehook.service.SubscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class IngestionServiceImpl implements IngestionService {
    private final SubscriptionService subscriptionService;
    private final EventRepository eventRepository;
    private final DeliveryRepository deliveryRepository;
    private final ApplicationEventPublisher applicationEventPublisher;


    public IngestionServiceImpl(SubscriptionService subscriptionService, EventRepository eventRepository, DeliveryRepository deliveryRepository, ApplicationEventPublisher applicationEventPublisher) {
        this.subscriptionService = subscriptionService;
        this.eventRepository = eventRepository;
        this.deliveryRepository = deliveryRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    @Transactional
    public void ingest(String ingestionId, String payload, Map<String, String> headers) {

        /**
         *
         * 1. Get the subscription with the ingestion id if not found, throw not found error
         * 2. Perform security validation of the payload based on the verification method on the subscription
         * 3. Create event and delivery in a transaction and enqueue a delivery*/


        // 1.
        Subscription subscription = subscriptionService.findByIngestionId(ingestionId);
        String idempotencyKey = this.generateIdempotencyKey(ingestionId, payload);

        Optional<Event> exitingEvent = eventRepository.findBySubscriptionIdAndIdempotencyKey(subscription.getId(), idempotencyKey);

        if (exitingEvent.isPresent()) {
            log.info("Duplicate event detected for subscription {}, idempotency key: {}",
                    subscription.getId(), idempotencyKey);
            return;
        }


//       2. TODO perform validation using strategy pattern based on verification type

//        3.
        Event event = eventRepository.save(Event.builder()
                .idempotencyKey(idempotencyKey)
                .receivedAt(OffsetDateTime.now())
                .payload(payload).
                headers(headers)
                .subscription(subscription).build());


        List<Delivery> deliveries = subscription.getEndpoints().stream()
                .filter((e) -> e.isActive())
                .map(e -> Delivery
                        .builder()
                        .endpoint(e)
                        .event(event)
                        .retryCount(0)
                        .status(DeliveryStatusEnum.QUEUED)
                        .build()).toList();

        List<Delivery> savedDeliveries = deliveryRepository.saveAll(deliveries);

        applicationEventPublisher
                .publishEvent(new DeliveriesCreatedEvent(savedDeliveries.stream().map((delivery -> delivery.getId())).toList()));
    }

    private String generateIdempotencyKey(String ingestionId, String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = ingestionId + ":" + payload;
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
