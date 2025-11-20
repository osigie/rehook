package com.osigie.rehook.domain;

import java.util.List;
import java.util.UUID;

public record DeliveriesCreatedEvent(List<UUID> deliveryIds) {
}
