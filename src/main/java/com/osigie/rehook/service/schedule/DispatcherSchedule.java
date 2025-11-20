package com.osigie.rehook.service.schedule;

import com.osigie.rehook.domain.DeliveriesCreatedEvent;
import com.osigie.rehook.service.DispatcherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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
    public void processRetries() {
//        TODO: think about tenancy state here
        //TODO: find due deliveries and reschedule
        log.info("checking for retries ......");
        List<UUID> deliveryIdList = dispatcherService.findRetries();

        log.info("due delivery ids  {}", deliveryIdList.size());

        applicationEventPublisher
                .publishEvent(new DeliveriesCreatedEvent(dispatcherService.findRetries()));
    }
}
