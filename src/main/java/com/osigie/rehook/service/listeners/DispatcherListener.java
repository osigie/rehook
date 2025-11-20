package com.osigie.rehook.service.listeners;


import com.osigie.rehook.domain.DeliveriesCreatedEvent;
import com.osigie.rehook.service.DispatcherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@Slf4j
public class DeliveriesCreatedListener {

    private final DispatcherService dispatcherService;

    public DeliveriesCreatedListener(DispatcherService dispatcherService) {
        this.dispatcherService = dispatcherService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDeliveriesCreated(DeliveriesCreatedEvent event) {
        log.info("Processing {} deliveries async", event.deliveryIds().size());

        dispatcherService.dispatchDeliveriesAsync(event.deliveryIds());
    }
}
