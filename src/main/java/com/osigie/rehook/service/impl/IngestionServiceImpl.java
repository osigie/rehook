package com.osigie.rehook.service.impl;

import com.osigie.rehook.model.*;
import com.osigie.rehook.repository.DeliveryRepository;
import com.osigie.rehook.repository.EventRepository;
import com.osigie.rehook.service.IngestionService;
import com.osigie.rehook.service.SubscriptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class IngestionServiceImpl implements IngestionService {
    private final SubscriptionService subscriptionService;
    private final EventRepository eventRepository;
    private final DeliveryRepository deliveryRepository;


    public IngestionServiceImpl(SubscriptionService subscriptionService, EventRepository eventRepository, DeliveryRepository deliveryRepository) {
        this.subscriptionService = subscriptionService;
        this.eventRepository = eventRepository;
        this.deliveryRepository = deliveryRepository;
    }

    @Override
    @Transactional
    public void ingest(String ingestionId, String payload, Map<String, String> headers) {

        /**
         * TODO: handle indempotency in case there is a retry from client
         * 1. Get the subscription with the ingestion id if not found, throw not found error
         * 2. Perform security validation of the payload based on the verification method on the subscription
         * 3. Create event and delivery in a transaction and enqueue a delivery*/

        System.out.println(ingestionId);

        // 1.
        Subscription subscription = subscriptionService.findByIngestionId(ingestionId);

//       2. TODO perform validation using strategy pattern based on verification type


//        3.
        Event event = eventRepository.save(Event.builder().receivedAt(OffsetDateTime.now()).payload(payload).
                headers(headers).subscription(subscription).build());


        //update next retry after failure
        List<Delivery> deliveries = subscription.getEndpoints().stream()
                .filter((e) -> e.isActive())
                .map(e -> Delivery
                        .builder()
                        .endpoint(e)
                        .event(event)
                        .status(DeliveryStatusEnum.QUEUED)
                        .build()).toList();

        deliveryRepository.saveAll(deliveries);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                System.out.println("Ran after commit");
                //enqueue
            }
        });
    }
}
