package com.osigie.rehook.dto.response;

import com.osigie.rehook.domain.model.DeliveryStatusEnum;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record DeliveryResponseDto(UUID id, DeliveryStatusEnum status, OffsetDateTime nextRetryAt, int retryCount,
                                  EventResponseDto event, EndpointResponseDto endpoint,
                                  Set<DeliveryAttemptResponseDto> deliveryAttempts) {
}
