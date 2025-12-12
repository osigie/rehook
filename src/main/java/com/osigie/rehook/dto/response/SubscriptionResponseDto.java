package com.osigie.rehook.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SubscriptionResponseDto(List<EndpointResponseDto> endpoints, String name, UUID id, String ingestionId, OffsetDateTime createdAt, OffsetDateTime updatedAt) {

}
