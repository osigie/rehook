package com.osigie.rehook.service.schedule;

import com.osigie.rehook.domain.DeliveriesCreatedEvent;
import com.osigie.rehook.service.DispatcherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class DispatcherSchedule {
    private final DispatcherService dispatcherService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public DispatcherSchedule(DispatcherService dispatcherService, ApplicationEventPublisher applicationEventPublisher) {
        this.dispatcherService = dispatcherService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Scheduled(fixedRate = 30_000)
    @Transactional(readOnly = true)
    public void processRetries() {
        Pageable pageable = PageRequest.of(0, 50);
        Page<UUID> batch = dispatcherService.findRetries(pageable);

        if (batch.isEmpty()) {
            log.info("No due deliveries found.");
            return;
        }

        log.info("Found {} deliveries to retry", batch.getContent().size());

        applicationEventPublisher.publishEvent(
                new DeliveriesCreatedEvent(batch.getContent())
        );
    }
}
