package com.osigie.rehook.service;

import java.util.List;
import java.util.UUID;

public interface DispatcherService {


    void dispatchDeliveriesAsync(List<UUID> deliveries);

    List<UUID> findRetries();
}
